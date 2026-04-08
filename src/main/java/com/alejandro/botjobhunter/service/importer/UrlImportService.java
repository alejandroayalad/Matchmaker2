package com.alejandro.botjobhunter.service.importer;

import com.alejandro.botjobhunter.dto.UrlImportResultDTO;
import com.alejandro.botjobhunter.models.Company;
import com.alejandro.botjobhunter.models.Job;
import com.alejandro.botjobhunter.models.enums.ExperienceLevel;
import com.alejandro.botjobhunter.repository.CompanyRepository;
import com.alejandro.botjobhunter.repository.JobRepository;
import com.alejandro.botjobhunter.service.JobDeduplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UrlImportService {
    private static final String DEFAULT_DESCRIPTION =
            "Imported from a job URL. Full job description was not available on the page.";
    private static final String DEFAULT_REQUIREMENTS =
            "Not available from URL import.";
    private static final String DEFAULT_SALARY = "Not specified";
    private static final String DEFAULT_RECRUITER_NAME = "Not available";

    private final List<UrlJobImporter> urlJobImporters;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final JobDeduplicationService jobDeduplicationService;

    public Job importFromUrl(String url) {
        String normalizedUrl = normalizeRequiredValue(url, "URL is required.");
        ImportAttempt importAttempt = resolveImportAttempt(normalizedUrl);
        Job job = mapToJob(importAttempt, normalizedUrl, LocalDateTime.now());
        List<Job> uniqueJobs = jobDeduplicationService.deduplicate(List.of(job));

        if (uniqueJobs.isEmpty()) {
            throw new IllegalStateException("Could not prepare a unique job for URL import: " + normalizedUrl);
        }

        Job uniqueJob = uniqueJobs.get(0);
        String companyName = uniqueJob.getCompany() != null
                ? normalizeCompanyName(uniqueJob.getCompany().getName())
                : normalizeCompanyName(null);

        if (jobRepository.existsByTitleIgnoreCaseAndCompany_NameIgnoreCaseAndUrlApplication(
                Objects.requireNonNullElse(uniqueJob.getTitle(), ""),
                companyName,
                Objects.requireNonNullElse(uniqueJob.getUrlApplication(), "")
        )) {
            throw new IllegalStateException("Job already exists for URL import: " + normalizedUrl);
        }

        return jobRepository.save(uniqueJob);
    }

    private ImportAttempt resolveImportAttempt(String url) {
        RuntimeException lastFailure = null;
        boolean anyMatchingImporter = false;

        for (UrlJobImporter importer : urlJobImporters) {
            if (!importer.canHandle(url)) {
                continue;
            }

            anyMatchingImporter = true;
            try {
                return new ImportAttempt(importer, importer.importFromUrl(url));
            } catch (RuntimeException exception) {
                lastFailure = exception;
            }
        }

        if (lastFailure != null) {
            throw new IllegalStateException("Failed to import job from URL: " + url, lastFailure);
        }

        if (!anyMatchingImporter) {
            throw new IllegalArgumentException("No URL importer can handle: " + url);
        }

        throw new IllegalStateException("Failed to import job from URL: " + url);
    }

    private Job mapToJob(ImportAttempt importAttempt, String originalUrl, LocalDateTime importedAt) {
        UrlImportResultDTO result = importAttempt.result();
        Company company = resolveCompany(result.companyName());

        return Job.builder()
                .title(result.title())
                .company(company)
                .location(result.location())
                .description(defaultIfBlank(result.description(), DEFAULT_DESCRIPTION))
                .requirements(DEFAULT_REQUIREMENTS)
                .urlApplication(defaultIfBlank(result.url(), originalUrl))
                .source(importAttempt.importer().getJobSource())
                .salary(defaultIfBlank(result.salary(), DEFAULT_SALARY))
                .recruiterName(DEFAULT_RECRUITER_NAME)
                .experienceLevel(inferExperienceLevel(result.title()))
                .scrappedAt(importedAt)
                .active(true)
                .build();
    }

    private Company resolveCompany(String companyName) {
        String normalizedCompanyName = normalizeCompanyName(companyName);
        return companyRepository.findByNameIgnoreCase(normalizedCompanyName)
                .orElseGet(() -> companyRepository.save(
                        Company.builder().name(normalizedCompanyName).build()
                ));
    }

    private String normalizeCompanyName(String companyName) {
        if (companyName == null || companyName.isBlank()) {
            return "Unknown Company";
        }

        return companyName.trim();
    }

    private ExperienceLevel inferExperienceLevel(String title) {
        if (title == null || title.isBlank()) {
            return ExperienceLevel.MID;
        }

        String normalizedTitle = title.toLowerCase(Locale.ROOT);
        if (containsAny(normalizedTitle, "intern", "entry", "trainee")) {
            return ExperienceLevel.ENTRY;
        }
        if (containsAny(normalizedTitle, "junior", "jr")) {
            return ExperienceLevel.JUNIOR;
        }
        if (containsAny(normalizedTitle, "senior", "sr", "staff", "principal", "lead")) {
            return ExperienceLevel.SENIOR;
        }
        return ExperienceLevel.MID;
    }

    private boolean containsAny(String value, String... tokens) {
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }

        return false;
    }

    private String normalizeRequiredValue(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }

    private String defaultIfBlank(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value.trim();
    }

    private record ImportAttempt(UrlJobImporter importer, UrlImportResultDTO result) {
    }
}
