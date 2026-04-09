package com.alejandro.botjobhunter.service.matching;

import com.alejandro.botjobhunter.dto.ScoreCard;
import com.alejandro.botjobhunter.models.Company;
import com.alejandro.botjobhunter.models.Job;
import com.alejandro.botjobhunter.models.JobSearchPreferences;
import com.alejandro.botjobhunter.models.User;
import com.alejandro.botjobhunter.models.UserSkill;
import com.alejandro.botjobhunter.models.enums.ExperienceLevel;
import com.alejandro.botjobhunter.models.enums.JobSource;
import com.alejandro.botjobhunter.models.enums.JobType;
import com.alejandro.botjobhunter.models.enums.ProficiencyLevel;
import com.alejandro.botjobhunter.models.enums.SkillCategory;
import com.alejandro.botjobhunter.repository.JobRepository;
import com.alejandro.botjobhunter.repository.JobSearchPreferencesRepository;
import com.alejandro.botjobhunter.repository.UserRepository;
import com.alejandro.botjobhunter.repository.UserSkillRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JobScoringServiceTest {

    @Test
    void getMatchedJobsShouldScoreSkillsBonusesAndNegativeSignals() {
        UserRepository userRepository = mock(UserRepository.class);
        UserSkillRepository userSkillRepository = mock(UserSkillRepository.class);
        JobSearchPreferencesRepository preferencesRepository = mock(JobSearchPreferencesRepository.class);
        JobRepository jobRepository = mock(JobRepository.class);

        JobScoringService service = new JobScoringService(
                userRepository,
                userSkillRepository,
                preferencesRepository,
                jobRepository
        );

        User user = User.builder().id(1L).firstName("Alejandro").build();
        JobSearchPreferences preferences = JobSearchPreferences.builder()
                .user(user)
                .targetSeniority(ExperienceLevel.MID)
                .preferredModality(JobType.REMOTE)
                .preferredLocation("Merida, Yucatan")
                .positiveKeywords(List.of("saas"))
                .targetRoleTitles(List.of("backend engineer"))
                .blacklistedCompanies(List.of("Teleperformance"))
                .negativeKeywords(List.of("php"))
                .build();
        List<UserSkill> userSkills = List.of(
                UserSkill.builder()
                        .skillName("Node.js")
                        .matchWeight(12)
                        .proficiencyLevel(ProficiencyLevel.ADVANCED)
                        .skillCategory(SkillCategory.FRAMEWORK)
                        .user(user)
                        .build(),
                UserSkill.builder()
                        .skillName("TypeScript")
                        .matchWeight(12)
                        .proficiencyLevel(ProficiencyLevel.ADVANCED)
                        .skillCategory(SkillCategory.LANGUAGE)
                        .user(user)
                        .build()
        );
        Job candidate = Job.builder()
                .Id(50L)
                .title("Backend Engineer - Node.js")
                .description("Build SaaS APIs with TypeScript. Requires 10+ years experience.")
                .company(Company.builder().name("Acme").build())
                .jobType(JobType.REMOTE)
                .experienceLevel(ExperienceLevel.MID)
                .source(JobSource.MANUAL)
                .active(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(preferencesRepository.findByUser_Id(1L)).thenReturn(Optional.of(preferences));
        when(userSkillRepository.findAllByUser_Id(1L)).thenReturn(userSkills);
        when(jobRepository.findMatchCandidates(
                eq(List.of(ExperienceLevel.SENIOR)),
                eq("Merida, Yucatan"),
                anyCollection(),
                anyCollection(),
                anyCollection()
        )).thenReturn(List.of(candidate));

        List<ScoreCard> result = service.getMatchedJobs(1L);

        assertEquals(1, result.size());
        ScoreCard scoreCard = result.getFirst();
        assertEquals(50L, scoreCard.jobId());
        assertEquals(53, scoreCard.totalScore());
        assertEquals(List.of("Node.js", "TypeScript"), scoreCard.skillMatches());
        assertEquals(List.of("High years-of-experience requirement: 10+ years"), scoreCard.negativeSignals());
        assertEquals(true, scoreCard.seniorityMatch());
        assertEquals(true, scoreCard.modalityMatch());
    }

    @Test
    void getMatchedJobsShouldFailWhenUserDoesNotExist() {
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        JobScoringService service = new JobScoringService(
                userRepository,
                mock(UserSkillRepository.class),
                mock(JobSearchPreferencesRepository.class),
                mock(JobRepository.class)
        );

        assertThrows(NoSuchElementException.class, () -> service.getMatchedJobs(99L));
    }
}
