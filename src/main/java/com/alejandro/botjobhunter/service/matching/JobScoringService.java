package com.alejandro.botjobhunter.service.matching;

import com.alejandro.botjobhunter.dto.ScoreCard;
import com.alejandro.botjobhunter.models.Job;
import com.alejandro.botjobhunter.models.JobSearchPreferences;
import com.alejandro.botjobhunter.models.User;
import com.alejandro.botjobhunter.models.UserSkill;
import com.alejandro.botjobhunter.models.enums.ExperienceLevel;
import com.alejandro.botjobhunter.models.enums.SkillCategory;
import com.alejandro.botjobhunter.repository.JobRepository;
import com.alejandro.botjobhunter.repository.JobSearchPreferencesRepository;
import com.alejandro.botjobhunter.repository.UserRepository;
import com.alejandro.botjobhunter.repository.UserSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class JobScoringService {
    private static final int TITLE_MATCH_MULTIPLIER = 2;
    private static final int MODALITY_MATCH_BONUS = 8;
    private static final int SENIORITY_MATCH_BONUS = 8;
    private static final int TARGET_ROLE_TITLE_BONUS = 6;
    private static final int POSITIVE_KEYWORD_BONUS = 3;
    private static final int COMPANY_PREFERENCE_BONUS = 4;

    private static final Map<String, List<String>> KNOWN_LANGUAGE_TOKENS = Map.of(
            "english", List.of("english", "ingles", "inglés"),
            "spanish", List.of("spanish", "espanol", "español"),
            "french", List.of("french", "frances", "francés"),
            "german", List.of("german", "aleman", "alemán", "deutsch"),
            "portuguese", List.of("portuguese", "portugues", "português"),
            "italian", List.of("italian", "italiano")
    );

    private static final List<NegativeSignalRule> NEGATIVE_SIGNAL_RULES = List.of(
            new NegativeSignalRule(
                    Pattern.compile("\\b(?:8|9|10|11|12)\\+?\\s+years?(?:\\s+of\\s+experience)?\\b"),
                    8,
                    "High years-of-experience requirement"
            ),
            new NegativeSignalRule(
                    Pattern.compile("\\b(?:principal|director|head of engineering)\\b"),
                    5,
                    "Leadership-heavy requirement"
            )
    );

    private final UserRepository userRepository;
    private final UserSkillRepository userSkillRepository;
    private final JobSearchPreferencesRepository jobSearchPreferencesRepository;
    private final JobRepository jobRepository;

    @Transactional(readOnly = true)
    public List<ScoreCard> getMatchedJobs(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        JobSearchPreferences preferences = jobSearchPreferencesRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new NoSuchElementException("Job search preferences not found for user: " + userId));
        List<UserSkill> userSkills = userSkillRepository.findAllByUser_Id(userId);

        List<Job> candidates = jobRepository.findMatchCandidates(
                resolveExcludedSeniorities(preferences.getTargetSeniority()),
                preferences.getPreferredLocation(),
                preferences.getBlacklistedCompanies(),
                preferences.getNegativeKeywords(),
                resolveUnsupportedLanguageTokens(userSkills)
        );

        return candidates.stream()
                .map(job -> scoreJob(job, preferences, userSkills))
                .sorted(Comparator
                        .comparingInt(ScoreCard::totalScore).reversed()
                        .thenComparing(ScoreCard::jobId))
                .toList();
    }

    ScoreCard scoreJob(Job job, JobSearchPreferences preferences, List<UserSkill> userSkills) {
        String titleText = normalize(job.getTitle());
        String descriptionText = normalize(job.getDescription());
        String companyName = normalize(job.getCompany() != null ? job.getCompany().getName() : null);

        int totalScore = 0;
        LinkedHashSet<String> skillMatches = new LinkedHashSet<>();
        List<String> negativeSignals = new ArrayList<>();
        List<String> breakdown = new ArrayList<>();

        for (UserSkill userSkill : userSkills) {
            MatchResult matchResult = calculateSkillMatch(userSkill, titleText, descriptionText);
            if (!matchResult.matched()) {
                continue;
            }

            totalScore += matchResult.points();
            skillMatches.add(userSkill.getSkillName());
            breakdown.add("+" + matchResult.points() + " " + userSkill.getSkillName() + " " + matchResult.reason());
        }

        boolean seniorityMatch = preferences.getTargetSeniority() != null
                && job.getExperienceLevel() == preferences.getTargetSeniority();
        if (seniorityMatch) {
            totalScore += SENIORITY_MATCH_BONUS;
            breakdown.add("+" + SENIORITY_MATCH_BONUS + " seniority alignment");
        }

        boolean modalityMatch = preferences.getPreferredModality() != null
                && job.getJobType() == preferences.getPreferredModality();
        if (modalityMatch) {
            totalScore += MODALITY_MATCH_BONUS;
            breakdown.add("+" + MODALITY_MATCH_BONUS + " modality alignment");
        }

        for (String roleTitle : safeList(preferences.getTargetRoleTitles())) {
            String normalizedRoleTitle = normalize(roleTitle);
            if (normalizedRoleTitle != null && containsKeyword(titleText, normalizedRoleTitle)) {
                totalScore += TARGET_ROLE_TITLE_BONUS;
                breakdown.add("+" + TARGET_ROLE_TITLE_BONUS + " target role title match: " + roleTitle);
            }
        }

        for (String keyword : safeList(preferences.getPositiveKeywords())) {
            String normalizedKeyword = normalize(keyword);
            if (normalizedKeyword == null) {
                continue;
            }

            if (containsKeyword(titleText, normalizedKeyword) || containsKeyword(descriptionText, normalizedKeyword)) {
                totalScore += POSITIVE_KEYWORD_BONUS;
                breakdown.add("+" + POSITIVE_KEYWORD_BONUS + " positive keyword match: " + keyword);
            }

            if (containsKeyword(companyName, normalizedKeyword)) {
                totalScore += COMPANY_PREFERENCE_BONUS;
                breakdown.add("+" + COMPANY_PREFERENCE_BONUS + " preferred company signal: " + keyword);
            }
        }

        for (NegativeSignalRule rule : NEGATIVE_SIGNAL_RULES) {
            if (descriptionText == null) {
                continue;
            }

            java.util.regex.Matcher matcher = rule.pattern().matcher(descriptionText);
            while (matcher.find()) {
                totalScore -= rule.penalty();
                String signal = rule.description() + ": " + matcher.group();
                negativeSignals.add(signal);
                breakdown.add("-" + rule.penalty() + " " + signal);
            }
        }

        return new ScoreCard(
                job.getId(),
                totalScore,
                new ArrayList<>(skillMatches),
                seniorityMatch,
                modalityMatch,
                negativeSignals,
                breakdown
        );
    }

    private MatchResult calculateSkillMatch(UserSkill userSkill, String titleText, String descriptionText) {
        String normalizedSkill = normalize(userSkill.getSkillName());
        if (normalizedSkill == null) {
            return MatchResult.notMatched();
        }

        boolean titleMatch = containsKeyword(titleText, normalizedSkill);
        boolean descriptionMatch = containsKeyword(descriptionText, normalizedSkill);
        if (!titleMatch && !descriptionMatch) {
            return MatchResult.notMatched();
        }

        int points = 0;
        if (titleMatch) {
            points += userSkill.getEffectiveMatchWeight() * TITLE_MATCH_MULTIPLIER;
        }
        if (descriptionMatch) {
            points += userSkill.getEffectiveMatchWeight();
        }

        String reason;
        if (titleMatch && descriptionMatch) {
            reason = "matched in title and description";
        } else if (titleMatch) {
            reason = "matched in title";
        } else {
            reason = "matched in description";
        }

        return new MatchResult(true, points, reason);
    }

    private List<String> resolveUnsupportedLanguageTokens(List<UserSkill> userSkills) {
        Set<String> supportedLanguages = new LinkedHashSet<>();

        for (UserSkill userSkill : userSkills) {
            if (userSkill.getSkillCategory() != SkillCategory.LANGUAGE) {
                continue;
            }

            String languageKey = resolveKnownLanguageKey(userSkill.getSkillName());
            if (languageKey != null) {
                supportedLanguages.add(languageKey);
            }
        }

        if (supportedLanguages.isEmpty()) {
            return List.of();
        }

        List<String> unsupported = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : KNOWN_LANGUAGE_TOKENS.entrySet()) {
            if (supportedLanguages.contains(entry.getKey())) {
                continue;
            }
            unsupported.addAll(entry.getValue());
        }

        return unsupported;
    }

    private List<ExperienceLevel> resolveExcludedSeniorities(ExperienceLevel targetSeniority) {
        if (targetSeniority == null) {
            return List.of();
        }

        return switch (targetSeniority) {
            case ENTRY -> List.of(ExperienceLevel.JUNIOR, ExperienceLevel.MID, ExperienceLevel.SENIOR);
            case JUNIOR -> List.of(ExperienceLevel.MID, ExperienceLevel.SENIOR);
            case MID -> List.of(ExperienceLevel.SENIOR);
            case SENIOR -> List.of();
        };
    }

    private String resolveKnownLanguageKey(String skillName) {
        String normalizedSkillName = normalize(skillName);
        if (normalizedSkillName == null) {
            return null;
        }

        for (Map.Entry<String, List<String>> entry : KNOWN_LANGUAGE_TOKENS.entrySet()) {
            for (String token : entry.getValue()) {
                if (containsKeyword(normalizedSkillName, normalize(token))) {
                    return entry.getKey();
                }
            }
        }

        return null;
    }

    private boolean containsKeyword(String text, String keyword) {
        if (text == null || keyword == null || keyword.isBlank()) {
            return false;
        }

        String simplifiedText = simplifyForSearch(text);
        String simplifiedKeyword = simplifyForSearch(keyword);
        String compactText = compactForSearch(text);
        String compactKeyword = compactForSearch(keyword);

        return text.contains(keyword)
                || (!simplifiedKeyword.isBlank() && simplifiedText.contains(simplifiedKeyword))
                || (!compactKeyword.isBlank() && compactText.contains(compactKeyword));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return stripAccents(value.toLowerCase(Locale.ROOT))
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String simplifyForSearch(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return "";
        }

        return normalized.replaceAll("[^\\p{L}\\p{N}]+", " ").replaceAll("\\s+", " ").trim();
    }

    private String compactForSearch(String value) {
        return simplifyForSearch(value).replace(" ", "");
    }

    private String stripAccents(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }

    private List<String> safeList(Collection<String> values) {
        return values == null ? List.of() : new ArrayList<>(values);
    }

    private record MatchResult(boolean matched, int points, String reason) {
        private static MatchResult notMatched() {
            return new MatchResult(false, 0, "");
        }
    }

    private record NegativeSignalRule(Pattern pattern, int penalty, String description) {
    }
}
