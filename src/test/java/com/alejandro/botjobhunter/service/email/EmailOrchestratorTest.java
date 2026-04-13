package com.alejandro.botjobhunter.service.email;

import com.alejandro.botjobhunter.dto.EmailParseResult;
import com.alejandro.botjobhunter.models.Company;
import com.alejandro.botjobhunter.models.EmailProcessingLog;
import com.alejandro.botjobhunter.models.Job;
import com.alejandro.botjobhunter.models.enums.EmailStatus;
import com.alejandro.botjobhunter.models.enums.JobSource;
import com.alejandro.botjobhunter.models.enums.JobType;
import com.alejandro.botjobhunter.repository.CompanyRepository;
import com.alejandro.botjobhunter.repository.EmailProcessingLogRepository;
import com.alejandro.botjobhunter.repository.JobRepository;
import com.alejandro.botjobhunter.service.JobDeduplicationService;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailOrchestratorTest {

    @Test
    void importLinkedInEmailsShouldSaveNewJobsAndLogProcessedMessages() throws Exception {
        EmailConnectionService emailConnectionService = mock(EmailConnectionService.class);
        MimeBodyExtractor mimeBodyExtractor = new MimeBodyExtractor();
        LinkedInEmailParser linkedInEmailParser = new LinkedInEmailParser();
        EmailCleanupService emailCleanupService = mock(EmailCleanupService.class);
        EmailProcessingLogRepository emailProcessingLogRepository = mock(EmailProcessingLogRepository.class);
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        JobRepository jobRepository = mock(JobRepository.class);

        EmailOrchestrator orchestrator = new EmailOrchestrator(
                emailConnectionService,
                mimeBodyExtractor,
                linkedInEmailParser,
                emailCleanupService,
                emailProcessingLogRepository,
                companyRepository,
                jobRepository,
                new JobDeduplicationService()
        );

        MimeMessage message = buildLinkedInMessage(
                "<message-1>",
                """
                        <table role="presentation" width="100%">
                          <tbody>
                            <tr>
                              <td>
                                <a href="https://www.linkedin.com/comm/jobs/view/4397726012/?trackingId=jobcard_body_0"
                                   style="color:#0a66c2;font-size:16px;font-weight:600;line-height:1.25">
                                  Junior Software Developer - Remote
                                </a>
                                <p style="margin:0;color:#1f1f1f;font-size:12px;line-height:1.25">
                                  Quik Hire Staffing · Mexico (En remoto)
                                </p>
                              </td>
                            </tr>
                          </tbody>
                        </table>
                        """
        );

        stubInbox(emailConnectionService, message);

        when(emailProcessingLogRepository.existsByMessageId("<message-1>")).thenReturn(false);
        when(companyRepository.findByNameIgnoreCase("Quik Hire Staffing")).thenReturn(Optional.empty());
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jobRepository.existsByTitleIgnoreCaseAndCompany_NameIgnoreCaseAndUrlApplication(
                eq("Junior Software Developer - Remote"),
                eq("Quik Hire Staffing"),
                eq("https://www.linkedin.com/jobs/view/4397726012")
        )).thenReturn(false);
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailProcessingLogRepository.save(any(EmailProcessingLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmailParseResult result = orchestrator.importLinkedInEmails(null);

        assertEquals(1, result.emailsScanned());
        assertEquals(1, result.emailsProcessed());
        assertEquals(1, result.jobsExtracted());
        assertEquals(1, result.jobsSaved());
        assertEquals(1, result.extractedJobs().size());
        assertEquals(1, result.savedJobs().size());
        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(jobCaptor.capture());
        Job savedJob = jobCaptor.getValue();
        assertEquals(JobSource.LINKEDIN_EMAIL, savedJob.getSource());
        assertEquals("Junior Software Developer - Remote", savedJob.getTitle());
        assertEquals("https://www.linkedin.com/jobs/view/4397726012", savedJob.getUrlApplication());
        assertEquals("Imported from LinkedIn email alert. Full job description was not included in the email.", savedJob.getDescription());
        assertEquals("Not available from LinkedIn email alert import.", savedJob.getRequirements());
        assertEquals("Not specified", savedJob.getSalary());
        assertEquals("Not available", savedJob.getRecruiterName());
        assertEquals(JobType.REMOTE, savedJob.getJobType());
        assertNotNull(savedJob.getCompany());
        assertEquals("Quik Hire Staffing", savedJob.getCompany().getName());

        ArgumentCaptor<EmailProcessingLog> logCaptor = ArgumentCaptor.forClass(EmailProcessingLog.class);
        verify(emailProcessingLogRepository).save(logCaptor.capture());
        EmailProcessingLog savedLog = logCaptor.getValue();
        assertEquals("<message-1>", savedLog.getMessageId());
        assertEquals(EmailStatus.PROCESSED, savedLog.getStatus());
        assertEquals(1, savedLog.getJobsExtracted());
        verify(emailCleanupService).cleanup(message);
    }

    @Test
    void importLinkedInEmailsShouldSkipAlreadyProcessedMessages() throws Exception {
        EmailConnectionService emailConnectionService = mock(EmailConnectionService.class);
        MimeBodyExtractor mimeBodyExtractor = mock(MimeBodyExtractor.class);
        LinkedInEmailParser linkedInEmailParser = new LinkedInEmailParser();
        EmailCleanupService emailCleanupService = mock(EmailCleanupService.class);
        EmailProcessingLogRepository emailProcessingLogRepository = mock(EmailProcessingLogRepository.class);
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        JobRepository jobRepository = mock(JobRepository.class);

        EmailOrchestrator orchestrator = new EmailOrchestrator(
                emailConnectionService,
                mimeBodyExtractor,
                linkedInEmailParser,
                emailCleanupService,
                emailProcessingLogRepository,
                companyRepository,
                jobRepository,
                new JobDeduplicationService()
        );

        MimeMessage message = buildLinkedInMessage("<message-2>", "<html></html>");
        stubInbox(emailConnectionService, message);

        when(emailProcessingLogRepository.existsByMessageId("<message-2>")).thenReturn(true);

        EmailParseResult result = orchestrator.importLinkedInEmails(null);

        assertEquals(new EmailParseResult(1, 0, 1, 0, 0, 0), result);
        verify(mimeBodyExtractor, never()).extractHtmlBody(any(Message.class));
        verify(jobRepository, never()).save(any(Job.class));
        verify(emailProcessingLogRepository, never()).save(any(EmailProcessingLog.class));
        verify(emailCleanupService, never()).cleanup(any(Message.class));
    }

    @Test
    void importLinkedInEmailsShouldLogFailedMessages() throws Exception {
        EmailConnectionService emailConnectionService = mock(EmailConnectionService.class);
        MimeBodyExtractor mimeBodyExtractor = mock(MimeBodyExtractor.class);
        LinkedInEmailParser linkedInEmailParser = new LinkedInEmailParser();
        EmailCleanupService emailCleanupService = mock(EmailCleanupService.class);
        EmailProcessingLogRepository emailProcessingLogRepository = mock(EmailProcessingLogRepository.class);
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        JobRepository jobRepository = mock(JobRepository.class);

        EmailOrchestrator orchestrator = new EmailOrchestrator(
                emailConnectionService,
                mimeBodyExtractor,
                linkedInEmailParser,
                emailCleanupService,
                emailProcessingLogRepository,
                companyRepository,
                jobRepository,
                new JobDeduplicationService()
        );

        MimeMessage message = buildLinkedInMessage("<message-3>", "<html></html>");
        stubInbox(emailConnectionService, message);

        when(emailProcessingLogRepository.existsByMessageId("<message-3>")).thenReturn(false);
        when(mimeBodyExtractor.extractHtmlBody(any(Message.class))).thenThrow(new IOException("boom"));
        when(emailProcessingLogRepository.save(any(EmailProcessingLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmailParseResult result = orchestrator.importLinkedInEmails(null);

        assertEquals(new EmailParseResult(1, 0, 0, 1, 0, 0), result);
        verify(jobRepository, never()).save(any(Job.class));

        ArgumentCaptor<EmailProcessingLog> logCaptor = ArgumentCaptor.forClass(EmailProcessingLog.class);
        verify(emailProcessingLogRepository).save(logCaptor.capture());
        EmailProcessingLog savedLog = logCaptor.getValue();
        assertEquals("<message-3>", savedLog.getMessageId());
        assertEquals(EmailStatus.FAILED, savedLog.getStatus());
        assertEquals(0, savedLog.getJobsExtracted());
        verify(emailCleanupService, never()).cleanup(any(Message.class));
    }

    @Test
    void importLinkedInEmailsShouldIgnoreCleanupFailuresAfterSuccessfulImport() throws Exception {
        EmailConnectionService emailConnectionService = mock(EmailConnectionService.class);
        MimeBodyExtractor mimeBodyExtractor = new MimeBodyExtractor();
        LinkedInEmailParser linkedInEmailParser = new LinkedInEmailParser();
        EmailCleanupService emailCleanupService = mock(EmailCleanupService.class);
        EmailProcessingLogRepository emailProcessingLogRepository = mock(EmailProcessingLogRepository.class);
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        JobRepository jobRepository = mock(JobRepository.class);

        EmailOrchestrator orchestrator = new EmailOrchestrator(
                emailConnectionService,
                mimeBodyExtractor,
                linkedInEmailParser,
                emailCleanupService,
                emailProcessingLogRepository,
                companyRepository,
                jobRepository,
                new JobDeduplicationService()
        );

        MimeMessage message = buildLinkedInMessage(
                "<message-4>",
                """
                        <table role="presentation" width="100%">
                          <tbody>
                            <tr>
                              <td>
                                <a href="https://www.linkedin.com/comm/jobs/view/4397726012/?trackingId=jobcard_body_0"
                                   style="color:#0a66c2;font-size:16px;font-weight:600;line-height:1.25">
                                  Senior Software Engineer
                                </a>
                                <p style="margin:0;color:#1f1f1f;font-size:12px;line-height:1.25">
                                  Acme Corp · Remote
                                </p>
                              </td>
                            </tr>
                          </tbody>
                        </table>
                        """
        );

        stubInbox(emailConnectionService, message);

        when(emailProcessingLogRepository.existsByMessageId("<message-4>")).thenReturn(false);
        when(companyRepository.findByNameIgnoreCase("Acme Corp")).thenReturn(Optional.empty());
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jobRepository.existsByTitleIgnoreCaseAndCompany_NameIgnoreCaseAndUrlApplication(
                eq("Senior Software Engineer"),
                eq("Acme Corp"),
                eq("https://www.linkedin.com/jobs/view/4397726012")
        )).thenReturn(false);
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailProcessingLogRepository.save(any(EmailProcessingLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.doThrow(new MessagingException("cleanup failed"))
                .when(emailCleanupService).cleanup(message);

        EmailParseResult result = orchestrator.importLinkedInEmails(null);

        assertEquals(1, result.emailsScanned());
        assertEquals(1, result.emailsProcessed());
        assertEquals(1, result.jobsExtracted());
        assertEquals(1, result.jobsSaved());
        verify(jobRepository).save(any(Job.class));
        verify(emailCleanupService).cleanup(message);
    }

    @SuppressWarnings("unchecked")
    private void stubInbox(EmailConnectionService emailConnectionService, Message... messages) throws MessagingException {
        Folder folder = mock(Folder.class);
        when(folder.search(any())).thenReturn(messages);
        when(emailConnectionService.withInbox(any())).thenAnswer(invocation -> {
            FolderOperation<Object> operation = (FolderOperation<Object>) invocation.getArgument(0);
            return operation.execute(folder);
        });
    }

    private MimeMessage buildLinkedInMessage(String messageId, String html) throws Exception {
        Session session = Session.getInstance(new Properties());
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("jobs-noreply@linkedin.com"));
        message.setSubject("LinkedIn job alert");
        message.setSentDate(new Date());
        message.setContent(html, "text/html; charset=UTF-8");
        message.saveChanges();
        message.setHeader("Message-ID", messageId);
        return message;
    }
}
