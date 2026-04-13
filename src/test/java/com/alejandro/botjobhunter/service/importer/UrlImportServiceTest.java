package com.alejandro.botjobhunter.service.importer;

import com.alejandro.botjobhunter.dto.UrlImportResultDTO;
import com.alejandro.botjobhunter.models.Company;
import com.alejandro.botjobhunter.models.Job;
import com.alejandro.botjobhunter.models.enums.ExperienceLevel;
import com.alejandro.botjobhunter.models.enums.JobSource;
import com.alejandro.botjobhunter.models.enums.JobType;
import com.alejandro.botjobhunter.repository.CompanyRepository;
import com.alejandro.botjobhunter.repository.JobRepository;
import com.alejandro.botjobhunter.service.JobDeduplicationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UrlImportServiceTest {

    @Test
    void importFromUrlShouldMapAndSaveImportedJob() {
        UrlJobImporter linkedInImporter = mock(UrlJobImporter.class);
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        JobRepository jobRepository = mock(JobRepository.class);

        UrlImportService service = new UrlImportService(
                List.of(linkedInImporter),
                companyRepository,
                jobRepository,
                new JobDeduplicationService()
        );

        UrlImportResultDTO parsedJob = new UrlImportResultDTO(
                "Senior Backend Engineer",
                "Acme Corp",
                "Remote, Mexico",
                "Build resilient APIs.",
                "MXN 90,000 - 110,000 monthly",
                "https://www.linkedin.com/jobs/view/1234567890/"
        );

        when(linkedInImporter.canHandle(parsedJob.url())).thenReturn(true);
        when(linkedInImporter.importFromUrl(parsedJob.url())).thenReturn(parsedJob);
        when(linkedInImporter.getJobSource()).thenReturn(JobSource.LINKEDIN_URL);
        when(companyRepository.findByNameIgnoreCase("Acme Corp")).thenReturn(Optional.empty());
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jobRepository.existsByTitleIgnoreCaseAndCompany_NameIgnoreCaseAndUrlApplication(
                eq("Senior Backend Engineer"),
                eq("Acme Corp"),
                eq("https://www.linkedin.com/jobs/view/1234567890/")
        )).thenReturn(false);
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Job savedJob = service.importFromUrl(parsedJob.url());

        assertEquals(JobSource.LINKEDIN_URL, savedJob.getSource());
        assertEquals("Senior Backend Engineer", savedJob.getTitle());
        assertEquals("Remote, Mexico", savedJob.getLocation());
        assertEquals("Build resilient APIs.", savedJob.getDescription());
        assertEquals("Not available from URL import.", savedJob.getRequirements());
        assertEquals("MXN 90,000 - 110,000 monthly", savedJob.getSalary());
        assertEquals("Not available", savedJob.getRecruiterName());
        assertEquals(ExperienceLevel.SENIOR, savedJob.getExperienceLevel());
        assertEquals(JobType.REMOTE, savedJob.getJobType());
        assertEquals(parsedJob.url(), savedJob.getUrlApplication());
        assertEquals(Boolean.TRUE, savedJob.getActive());
        assertNotNull(savedJob.getScrappedAt());
        assertNotNull(savedJob.getCompany());
        assertEquals("Acme Corp", savedJob.getCompany().getName());

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(jobCaptor.capture());
        assertEquals(JobSource.LINKEDIN_URL, jobCaptor.getValue().getSource());
    }

    @Test
    void importFromUrlShouldUseFallbackImporterWhenPrimaryParserFails() {
        UrlJobImporter linkedInImporter = mock(UrlJobImporter.class);
        UrlJobImporter genericImporter = mock(UrlJobImporter.class);
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        JobRepository jobRepository = mock(JobRepository.class);

        UrlImportService service = new UrlImportService(
                List.of(linkedInImporter, genericImporter),
                companyRepository,
                jobRepository,
                new JobDeduplicationService()
        );

        String url = "https://www.linkedin.com/jobs/view/1234567890/";
        UrlImportResultDTO parsedJob = new UrlImportResultDTO(
                "Platform Engineer",
                "Fallback Co",
                "Remote",
                null,
                null,
                url
        );
        Company existingCompany = Company.builder().name("Fallback Co").build();

        when(linkedInImporter.canHandle(url)).thenReturn(true);
        when(linkedInImporter.importFromUrl(url)).thenThrow(new IllegalStateException("LinkedIn selectors changed"));
        when(genericImporter.canHandle(url)).thenReturn(true);
        when(genericImporter.importFromUrl(url)).thenReturn(parsedJob);
        when(genericImporter.getJobSource()).thenReturn(JobSource.MANUAL);
        when(companyRepository.findByNameIgnoreCase("Fallback Co")).thenReturn(Optional.of(existingCompany));
        when(jobRepository.existsByTitleIgnoreCaseAndCompany_NameIgnoreCaseAndUrlApplication(
                eq("Platform Engineer"),
                eq("Fallback Co"),
                eq(url)
        )).thenReturn(false);
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Job savedJob = service.importFromUrl(url);

        assertEquals(JobSource.MANUAL, savedJob.getSource());
        assertEquals("Imported from a job URL. Full job description was not available on the page.", savedJob.getDescription());
        assertEquals("Not specified", savedJob.getSalary());
    }

    @Test
    void importFromUrlShouldRejectDuplicateJobs() {
        UrlJobImporter genericImporter = mock(UrlJobImporter.class);
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        JobRepository jobRepository = mock(JobRepository.class);

        UrlImportService service = new UrlImportService(
                List.of(genericImporter),
                companyRepository,
                jobRepository,
                new JobDeduplicationService()
        );

        String url = "https://careers.example.com/jobs/123";
        UrlImportResultDTO parsedJob = new UrlImportResultDTO(
                "Backend Engineer",
                "Acme Corp",
                "Mexico City",
                "Build APIs.",
                null,
                url
        );
        Company existingCompany = Company.builder().name("Acme Corp").build();

        when(genericImporter.canHandle(url)).thenReturn(true);
        when(genericImporter.importFromUrl(url)).thenReturn(parsedJob);
        when(genericImporter.getJobSource()).thenReturn(JobSource.MANUAL);
        when(companyRepository.findByNameIgnoreCase("Acme Corp")).thenReturn(Optional.of(existingCompany));
        when(jobRepository.existsByTitleIgnoreCaseAndCompany_NameIgnoreCaseAndUrlApplication(
                eq("Backend Engineer"),
                eq("Acme Corp"),
                eq(url)
        )).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.importFromUrl(url));
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void importFromUrlShouldRejectUrlsWithoutSupportingImporter() {
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        JobRepository jobRepository = mock(JobRepository.class);

        UrlImportService service = new UrlImportService(
                List.of(),
                companyRepository,
                jobRepository,
                new JobDeduplicationService()
        );

        assertThrows(IllegalArgumentException.class, () -> service.importFromUrl("https://example.com/jobs/123"));
        verify(jobRepository, never()).save(any(Job.class));
    }
}
