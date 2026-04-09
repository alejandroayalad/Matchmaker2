package com.alejandro.botjobhunter.repository;

import com.alejandro.botjobhunter.models.Company;
import com.alejandro.botjobhunter.models.Job;
import com.alejandro.botjobhunter.models.enums.ExperienceLevel;
import com.alejandro.botjobhunter.models.enums.JobType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Repository
public class JobRepositoryImpl implements JobRepositoryCustom {
    private static final String DEFAULT_ONSITE_CITY = "merida";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Job> findMatchCandidates(
            ExperienceLevel targetSeniority,
            String preferredLocation,
            Collection<String> blacklistedCompanies,
            Collection<String> negativeTitleKeywords,
            Collection<String> unsupportedLanguageTokens
    ) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Job> criteriaQuery = criteriaBuilder.createQuery(Job.class);
        Root<Job> job = criteriaQuery.from(Job.class);
        job.fetch("company", JoinType.LEFT);
        Join<Job, Company> company = job.join("company", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.isTrue(job.get("active")));
        predicates.add(buildOnsiteLocationPredicate(criteriaBuilder, job, preferredLocation));

        if (targetSeniority != null) {
            predicates.add(criteriaBuilder.or(
                    criteriaBuilder.isNull(job.get("experienceLevel")),
                    criteriaBuilder.equal(job.get("experienceLevel"), targetSeniority)
            ));
        }

        List<String> normalizedBlacklistedCompanies = normalizeTokens(blacklistedCompanies);
        if (!normalizedBlacklistedCompanies.isEmpty()) {
            Expression<String> companyName = criteriaBuilder.lower(criteriaBuilder.coalesce(company.get("name"), ""));
            predicates.add(criteriaBuilder.or(
                    criteriaBuilder.isNull(company.get("name")),
                    criteriaBuilder.not(companyName.in(normalizedBlacklistedCompanies))
            ));
        }

        List<String> normalizedNegativeTitleKeywords = normalizeTokens(negativeTitleKeywords);
        if (!normalizedNegativeTitleKeywords.isEmpty()) {
            Expression<String> titleText = criteriaBuilder.lower(criteriaBuilder.coalesce(job.get("title"), ""));
            predicates.add(criteriaBuilder.not(containsAny(criteriaBuilder, titleText, normalizedNegativeTitleKeywords)));
        }

        List<String> normalizedUnsupportedLanguageTokens = normalizeTokens(unsupportedLanguageTokens);
        if (!normalizedUnsupportedLanguageTokens.isEmpty()) {
            Expression<String> titleAndDescription = criteriaBuilder.lower(
                    criteriaBuilder.concat(
                            criteriaBuilder.concat(criteriaBuilder.coalesce(job.get("title"), ""), " "),
                            criteriaBuilder.coalesce(job.get("description"), "")
                    )
            );
            predicates.add(criteriaBuilder.not(
                    containsAny(criteriaBuilder, titleAndDescription, normalizedUnsupportedLanguageTokens)
            ));
        }

        criteriaQuery.select(job)
                .distinct(true)
                .where(predicates.toArray(Predicate[]::new))
                .orderBy(
                        criteriaBuilder.desc(job.get("scrappedAt")),
                        criteriaBuilder.asc(job.get("Id"))
                );

        return entityManager.createQuery(criteriaQuery).getResultList();
    }

    private Predicate buildOnsiteLocationPredicate(
            CriteriaBuilder criteriaBuilder,
            Root<Job> job,
            String preferredLocation
    ) {
        Expression<String> location = criteriaBuilder.lower(criteriaBuilder.coalesce(job.get("location"), ""));
        List<String> allowedOnsiteTokens = resolveAllowedOnsiteTokens(preferredLocation);

        return criteriaBuilder.or(
                criteriaBuilder.isNull(job.get("jobType")),
                criteriaBuilder.notEqual(job.get("jobType"), JobType.ONSITE),
                containsAny(criteriaBuilder, location, allowedOnsiteTokens)
        );
    }

    private Predicate containsAny(
            CriteriaBuilder criteriaBuilder,
            Expression<String> textExpression,
            Collection<String> tokens
    ) {
        List<Predicate> matches = new ArrayList<>();
        for (String token : tokens) {
            matches.add(criteriaBuilder.like(textExpression, "%" + token + "%"));
        }

        return criteriaBuilder.or(matches.toArray(Predicate[]::new));
    }

    private List<String> resolveAllowedOnsiteTokens(String preferredLocation) {
        String city = preferredLocation;
        if (city == null || city.isBlank()) {
            city = DEFAULT_ONSITE_CITY;
        } else {
            city = city.split(",", 2)[0];
        }

        List<String> tokens = normalizeTokens(List.of(city));
        if (tokens.isEmpty()) {
            tokens.add(DEFAULT_ONSITE_CITY);
        }

        return tokens;
    }

    private List<String> normalizeTokens(Collection<String> values) {
        List<String> normalized = new ArrayList<>();
        if (values == null) {
            return normalized;
        }

        for (String value : values) {
            String lowered = lower(value);
            if (lowered != null && !normalized.contains(lowered)) {
                normalized.add(lowered);
            }

            String accentless = stripAccents(lowered);
            if (accentless != null && !normalized.contains(accentless)) {
                normalized.add(accentless);
            }
        }

        return normalized;
    }

    private String lower(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String lowered = value.toLowerCase(Locale.ROOT).trim();
        return lowered.isBlank() ? null : lowered;
    }

    private String stripAccents(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }
}
