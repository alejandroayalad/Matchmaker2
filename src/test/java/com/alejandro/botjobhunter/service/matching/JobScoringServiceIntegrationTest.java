package com.alejandro.botjobhunter.service.matching;

import com.alejandro.botjobhunter.dto.ScoreCard;
import com.alejandro.botjobhunter.models.Company;
import com.alejandro.botjobhunter.models.Job;
import com.alejandro.botjobhunter.models.User;
import com.alejandro.botjobhunter.models.enums.ExperienceLevel;
import com.alejandro.botjobhunter.models.enums.JobSource;
import com.alejandro.botjobhunter.models.enums.JobType;
import com.alejandro.botjobhunter.repository.CompanyRepository;
import com.alejandro.botjobhunter.repository.JobRepository;
import com.alejandro.botjobhunter.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class JobScoringServiceIntegrationTest {

    @Autowired
    private JobScoringService jobScoringService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    void getMatchedJobsShouldLoadPreferencesCollectionsInsideServiceTransaction() {
        User user = userRepository.findAll().stream().findFirst().orElseThrow();
        Company company = companyRepository.save(Company.builder().name("Acme Platform").build());

        jobRepository.save(Job.builder()
                .title("Backend Engineer - Node.js")
                .description("Build SaaS APIs with TypeScript and Docker.")
                .company(company)
                .jobType(JobType.REMOTE)
                .experienceLevel(ExperienceLevel.MID)
                .location("Remote")
                .source(JobSource.MANUAL)
                .active(true)
                .build());

        List<ScoreCard> results = jobScoringService.getMatchedJobs(user.getId());

        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(scoreCard -> scoreCard.totalScore() > 0));
    }
}
