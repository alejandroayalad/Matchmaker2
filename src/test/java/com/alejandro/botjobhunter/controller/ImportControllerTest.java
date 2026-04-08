package com.alejandro.botjobhunter.controller;

import com.alejandro.botjobhunter.dto.EmailParseResult;
import com.alejandro.botjobhunter.dto.UrlImportRequest;
import com.alejandro.botjobhunter.models.EmailProcessingLog;
import com.alejandro.botjobhunter.models.Job;
import com.alejandro.botjobhunter.models.enums.EmailStatus;
import com.alejandro.botjobhunter.models.enums.JobSource;
import com.alejandro.botjobhunter.repository.EmailProcessingLogRepository;
import com.alejandro.botjobhunter.service.email.EmailOrchestrator;
import com.alejandro.botjobhunter.service.importer.UrlImportService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImportControllerTest {

    @Test
    void importLinkedInEmailsShouldReturnImportSummary() throws Exception {
        EmailOrchestrator emailOrchestrator = mock(EmailOrchestrator.class);
        EmailProcessingLogRepository emailProcessingLogRepository = mock(EmailProcessingLogRepository.class);
        UrlImportService urlImportService = mock(UrlImportService.class);

        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("emailOrchestrator", emailOrchestrator);

        ImportController controller = new ImportController(
                beanFactory.getBeanProvider(EmailOrchestrator.class),
                emailProcessingLogRepository,
                urlImportService
        );

        EmailParseResult expected = new EmailParseResult(3, 2, 1, 0, 5, 4);
        when(emailOrchestrator.importLinkedInEmails(null)).thenReturn(expected);

        EmailParseResult result = controller.importLinkedInEmails(null);

        assertEquals(expected, result);
        verify(emailOrchestrator).importLinkedInEmails(null);
    }

    @Test
    void importLinkedInEmailsShouldReturnServiceUnavailableWhenMailIsDisabled() {
        EmailProcessingLogRepository emailProcessingLogRepository = mock(EmailProcessingLogRepository.class);
        UrlImportService urlImportService = mock(UrlImportService.class);
        ObjectProvider<EmailOrchestrator> provider = new StaticListableBeanFactory()
                .getBeanProvider(EmailOrchestrator.class);

        ImportController controller = new ImportController(provider, emailProcessingLogRepository, urlImportService);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.importLinkedInEmails(null)
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
    }

    @Test
    void getEmailImportLogShouldApplyStatusFilterAndPagination() {
        EmailProcessingLogRepository emailProcessingLogRepository = mock(EmailProcessingLogRepository.class);
        UrlImportService urlImportService = mock(UrlImportService.class);
        ImportController controller = new ImportController(
                new StaticListableBeanFactory().getBeanProvider(EmailOrchestrator.class),
                emailProcessingLogRepository,
                urlImportService
        );

        EmailProcessingLog log = EmailProcessingLog.builder()
                .messageId("<message-1>")
                .subject("LinkedIn Job Alert")
                .sender("jobs-noreply@linkedin.com")
                .processedAt(LocalDateTime.now())
                .jobsExtracted(2)
                .status(EmailStatus.PROCESSED)
                .build();

        Page<EmailProcessingLog> expectedPage = new PageImpl<>(List.of(log));
        when(emailProcessingLogRepository.findAllByStatus(eq(EmailStatus.PROCESSED), any(Pageable.class)))
                .thenReturn(expectedPage);

        Page<EmailProcessingLog> result = controller.getEmailImportLog(1, 250, EmailStatus.PROCESSED);

        assertEquals(expectedPage, result);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(emailProcessingLogRepository).findAllByStatus(eq(EmailStatus.PROCESSED), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertEquals(1, pageable.getPageNumber());
        assertEquals(100, pageable.getPageSize());
        assertEquals("processedAt: DESC", pageable.getSort().toString());
    }

    @Test
    void importJobFromUrlShouldReturnSavedJob() {
        EmailProcessingLogRepository emailProcessingLogRepository = mock(EmailProcessingLogRepository.class);
        UrlImportService urlImportService = mock(UrlImportService.class);
        ImportController controller = new ImportController(
                new StaticListableBeanFactory().getBeanProvider(EmailOrchestrator.class),
                emailProcessingLogRepository,
                urlImportService
        );

        UrlImportRequest request = new UrlImportRequest("https://www.linkedin.com/jobs/view/1234567890/");
        Job expectedJob = Job.builder()
                .title("Senior Backend Engineer")
                .urlApplication(request.url())
                .source(JobSource.LINKEDIN_URL)
                .active(true)
                .build();

        when(urlImportService.importFromUrl(request.url())).thenReturn(expectedJob);

        Job result = controller.importJobFromUrl(request);

        assertEquals(expectedJob, result);
        verify(urlImportService).importFromUrl(request.url());
    }

    @Test
    void importJobFromUrlShouldReturnBadRequestForInvalidUrl() {
        EmailProcessingLogRepository emailProcessingLogRepository = mock(EmailProcessingLogRepository.class);
        UrlImportService urlImportService = mock(UrlImportService.class);
        ImportController controller = new ImportController(
                new StaticListableBeanFactory().getBeanProvider(EmailOrchestrator.class),
                emailProcessingLogRepository,
                urlImportService
        );

        UrlImportRequest request = new UrlImportRequest("   ");
        when(urlImportService.importFromUrl(request.url())).thenThrow(new IllegalArgumentException("URL is required."));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.importJobFromUrl(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void importJobFromUrlShouldReturnConflictWhenJobAlreadyExists() {
        EmailProcessingLogRepository emailProcessingLogRepository = mock(EmailProcessingLogRepository.class);
        UrlImportService urlImportService = mock(UrlImportService.class);
        ImportController controller = new ImportController(
                new StaticListableBeanFactory().getBeanProvider(EmailOrchestrator.class),
                emailProcessingLogRepository,
                urlImportService
        );

        UrlImportRequest request = new UrlImportRequest("https://careers.example.com/jobs/123");
        when(urlImportService.importFromUrl(request.url()))
                .thenThrow(new IllegalStateException("Job already exists for URL import: " + request.url()));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.importJobFromUrl(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }
}
