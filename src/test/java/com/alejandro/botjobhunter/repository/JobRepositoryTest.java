package com.alejandro.botjobhunter.repository;

import com.alejandro.botjobhunter.models.Company;
import com.alejandro.botjobhunter.models.Job;
import com.alejandro.botjobhunter.models.enums.ExperienceLevel;
import com.alejandro.botjobhunter.models.enums.JobSource;
import com.alejandro.botjobhunter.models.enums.JobType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class JobRepositoryTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    void findMatchCandidatesShouldApplyHardFiltersInDatabase() {
        Company goodCompany = companyRepository.save(Company.builder().name("Acme").build());
        Company blacklistedCompany = companyRepository.save(Company.builder().name("Teleperformance").build());

        jobRepository.saveAll(List.of(
                job("Backend Engineer", "TypeScript and Node.js role", goodCompany, JobType.REMOTE, ExperienceLevel.MID, "Remote"),
                job("Backend Engineer", "Onsite in Merida", goodCompany, JobType.ONSITE, ExperienceLevel.MID, "Merida, Yucatan"),
                job("Backend Engineer", "Onsite in Mexico City", goodCompany, JobType.ONSITE, ExperienceLevel.MID, "Mexico City"),
                job("Senior Backend Engineer", "Staff platform work", goodCompany, JobType.REMOTE, ExperienceLevel.SENIOR, "Remote"),
                job("PHP Developer", "Legacy PHP stack", goodCompany, JobType.REMOTE, ExperienceLevel.MID, "Remote"),
                job("Backend Engineer", "German language required", goodCompany, JobType.REMOTE, ExperienceLevel.MID, "Remote"),
                job("Backend Engineer", "Strong Java backend role", blacklistedCompany, JobType.REMOTE, ExperienceLevel.MID, "Remote")
        ));

        List<Job> candidates = jobRepository.findMatchCandidates(
                List.of(ExperienceLevel.SENIOR),
                "Merida, Yucatan",
                List.of("Teleperformance"),
                List.of("php"),
                List.of("german")
        );

        assertEquals(2, candidates.size());
        assertEquals(
                List.of("Merida, Yucatan", "Remote"),
                candidates.stream().map(Job::getLocation).sorted().collect(Collectors.toList())
        );
    }

    private Job job(
            String title,
            String description,
            Company company,
            JobType jobType,
            ExperienceLevel experienceLevel,
            String location
    ) {
        return Job.builder()
                .title(title)
                .description(description)
                .company(company)
                .jobType(jobType)
                .experienceLevel(experienceLevel)
                .location(location)
                .source(JobSource.MANUAL)
                .active(true)
                .build();
    }
}
