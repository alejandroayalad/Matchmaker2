package com.alejandro.botjobhunter.service.email;

import com.alejandro.botjobhunter.dto.EmailParseResult;
import com.alejandro.botjobhunter.models.Company;
import com.alejandro.botjobhunter.models.EmailProcessingLog;
import com.alejandro.botjobhunter.models.Job;
import com.alejandro.botjobhunter.models.enums.ExperienceLevel;
import com.alejandro.botjobhunter.models.enums.EmailStatus;
import com.alejandro.botjobhunter.models.enums.JobSource;
import com.alejandro.botjobhunter.repository.CompanyRepository;
import com.alejandro.botjobhunter.repository.EmailProcessingLogRepository;
import com.alejandro.botjobhunter.repository.JobRepository;
import com.alejandro.botjobhunter.service.JobDeduplicationService;
import com.alejandro.botjobhunter.dto.EmailJobResultDTO;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.FromStringTerm;
import jakarta.mail.search.OrTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;
import jakarta.mail.search.SubjectTerm;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@ConditionalOnProperty(prefix = "mail", name = "enabled", havingValue = "true")
public class EmailOrchestrator {
    private static final String DEFAULT_DESCRIPTION =
            "Imported from LinkedIn email alert. Full job description was not included in the email.";
    private static final String DEFAULT_REQUIREMENTS =
            "Not available from LinkedIn email alert import.";
    private static final String DEFAULT_SALARY = "Not specified";
    private static final String DEFAULT_RECRUITER_NAME = "Not available";

    private final EmailConnectionService emailConnectionService;
    private final MimeBodyExtractor mimeBodyExtractor;
    private final LinkedInEmailParser linkedInEmailParser;
    private final EmailCleanupService emailCleanupService;
    private final EmailProcessingLogRepository emailProcessingLogRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final JobDeduplicationService jobDeduplicationService;

    public EmailOrchestrator(
            EmailConnectionService emailConnectionService,
            MimeBodyExtractor mimeBodyExtractor,
            LinkedInEmailParser linkedInEmailParser,
            EmailCleanupService emailCleanupService,
            EmailProcessingLogRepository emailProcessingLogRepository,
            CompanyRepository companyRepository,
            JobRepository jobRepository,
            JobDeduplicationService jobDeduplicationService
    ) {
        this.emailConnectionService = emailConnectionService;
        this.mimeBodyExtractor = mimeBodyExtractor;
        this.linkedInEmailParser = linkedInEmailParser;
        this.emailCleanupService = emailCleanupService;
        this.emailProcessingLogRepository = emailProcessingLogRepository;
        this.companyRepository = companyRepository;
        this.jobRepository = jobRepository;
        this.jobDeduplicationService = jobDeduplicationService;
    }

    public EmailParseResult importLinkedInEmails(Date since) throws MessagingException {
        return emailConnectionService.withInbox(folder -> {
            Message[] messages = folder.search(buildLinkedInSearchTerm(since));
            EmailImportAccumulator accumulator = new EmailImportAccumulator(messages.length);

            for (Message message : messages) {
                processMessage(message, accumulator);
            }

            return accumulator.toResult();
        });
    }

    private void processMessage(Message message, EmailImportAccumulator accumulator) {
        String messageId = resolveMessageId(message);

        if (emailProcessingLogRepository.existsByMessageId(messageId)) {
            accumulator.incrementSkipped();
            return;
        }

        String subject = safeSubject(message);
        String sender = resolveSender(message);
        LocalDateTime processedAt = LocalDateTime.now();

        try {
            String htmlBody = mimeBodyExtractor.extractHtmlBody(message);
            if (htmlBody == null || htmlBody.isBlank()) {
                saveLog(messageId, subject, sender, processedAt, 0, EmailStatus.SKIPPED);
                accumulator.incrementSkipped();
                return;
            }

            if (!linkedInEmailParser.isLikelyJobAlertEmail(subject, sender, htmlBody)) {
                saveLog(messageId, subject, sender, processedAt, 0, EmailStatus.SKIPPED);
                accumulator.incrementSkipped();
                return;
            }

            List<EmailJobResultDTO> parsedJobs = linkedInEmailParser.parse(htmlBody);
            if (parsedJobs.isEmpty()) {
                saveLog(messageId, subject, sender, processedAt, 0, EmailStatus.SKIPPED);
                accumulator.incrementSkipped();
                return;
            }

            accumulator.addExtractedJobs(parsedJobs.size());

            List<Job> jobsToImport = mapToJobs(parsedJobs, processedAt);
            List<Job> uniqueJobs = jobDeduplicationService.deduplicate(jobsToImport);
            int jobsSaved = saveNewJobs(uniqueJobs);

            saveLog(
                    messageId,
                    subject,
                    sender,
                    processedAt,
                    parsedJobs.size(),
                    jobsSaved > 0 ? EmailStatus.PROCESSED : EmailStatus.SKIPPED
            );

            if (jobsSaved > 0) {
                cleanupProcessedMessage(message);
                accumulator.incrementProcessed();
                accumulator.addSavedJobs(jobsSaved);
            } else {
                accumulator.incrementSkipped();
            }
        } catch (MessagingException | IOException exception) {
            saveLog(messageId, subject, sender, processedAt, 0, EmailStatus.FAILED);
            accumulator.incrementFailed();
        } catch (RuntimeException exception) {
            saveLog(messageId, subject, sender, processedAt, 0, EmailStatus.FAILED);
            accumulator.incrementFailed();
        }
    }

    private void cleanupProcessedMessage(Message message) {
        try {
            emailCleanupService.cleanup(message);
        } catch (MessagingException exception) {
            System.err.println("Email cleanup failed: " + exception.getMessage());
        }
    }

    private List<Job> mapToJobs(List<EmailJobResultDTO> parsedJobs, LocalDateTime processedAt) {
        Map<String, Company> companyCache = new HashMap<>();
        List<Job> jobs = new ArrayList<>();

        for (EmailJobResultDTO parsedJob : parsedJobs) {
            Company company = resolveCompany(parsedJob.companyName(), companyCache);
            Job job = Job.builder()
                    .title(parsedJob.title())
                    .company(company)
                    .location(parsedJob.location())
                    .description(DEFAULT_DESCRIPTION)
                    .requirements(DEFAULT_REQUIREMENTS)
                    .urlApplication(parsedJob.url())
                    .source(JobSource.LINKEDIN_EMAIL)
                    .salary(DEFAULT_SALARY)
                    .recruiterName(DEFAULT_RECRUITER_NAME)
                    .experienceLevel(inferExperienceLevel(parsedJob.title()))
                    .scrappedAt(processedAt)
                    .active(true)
                    .build();

            jobs.add(job);
        }

        return jobs;
    }

    private Company resolveCompany(String companyName, Map<String, Company> companyCache) {
        String normalizedCompanyName = normalizeCompanyName(companyName);
        Company cachedCompany = companyCache.get(normalizedCompanyName.toLowerCase(Locale.ROOT));
        if (cachedCompany != null) {
            return cachedCompany;
        }

        Company company = companyRepository.findByNameIgnoreCase(normalizedCompanyName)
                .orElseGet(() -> companyRepository.save(
                        Company.builder().name(normalizedCompanyName).build()
                ));

        companyCache.put(normalizedCompanyName.toLowerCase(Locale.ROOT), company);
        return company;
    }

    private int saveNewJobs(List<Job> jobs) {
        int savedCount = 0;

        for (Job job : jobs) {
            String companyName = job.getCompany() != null ? job.getCompany().getName() : normalizeCompanyName(null);
            if (jobRepository.existsByTitleIgnoreCaseAndCompany_NameIgnoreCaseAndUrlApplication(
                    Objects.requireNonNullElse(job.getTitle(), ""),
                    companyName,
                    Objects.requireNonNullElse(job.getUrlApplication(), "")
            )) {
                continue;
            }

            jobRepository.save(job);
            savedCount++;
        }

        return savedCount;
    }

    private void saveLog(
            String messageId,
            String subject,
            String sender,
            LocalDateTime processedAt,
            int jobsExtracted,
            EmailStatus status
    ) {
        emailProcessingLogRepository.save(
                EmailProcessingLog.builder()
                        .messageId(messageId)
                        .subject(subject)
                        .sender(sender)
                        .processedAt(processedAt)
                        .jobsExtracted(jobsExtracted)
                        .status(status)
                        .build()
        );
    }

    private String resolveMessageId(Message message) {
        try {
            String[] headers = message.getHeader("Message-ID");
            if (headers != null && headers.length > 0 && headers[0] != null && !headers[0].isBlank()) {
                return headers[0].trim();
            }
        } catch (MessagingException ignored) {
        }

        return "generated:" + Objects.hash(
                safeSubject(message),
                resolveSender(message),
                safeSentTimestamp(message)
        );
    }

    private String safeSubject(Message message) {
        try {
            return message.getSubject();
        } catch (MessagingException ignored) {
            return null;
        }
    }

    private String resolveSender(Message message) {
        try {
            Address[] fromAddresses = message.getFrom();
            if (fromAddresses == null || fromAddresses.length == 0) {
                return null;
            }
            return fromAddresses[0].toString();
        } catch (MessagingException ignored) {
            return null;
        }
    }

    private Long safeSentTimestamp(Message message) {
        try {
            Date sentDate = message.getSentDate();
            return sentDate != null ? sentDate.getTime() : null;
        } catch (MessagingException ignored) {
            return null;
        }
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

    private SearchTerm buildLinkedInSearchTerm(Date since) {
        SearchTerm fromLinkedIn = new FromStringTerm("linkedin");
        SearchTerm subjectLinkedIn = new SubjectTerm("LinkedIn");
        SearchTerm linkedInTerm = new OrTerm(fromLinkedIn, subjectLinkedIn);

        if (since == null) {
            return linkedInTerm;
        }

        SearchTerm receivedSince = new ReceivedDateTerm(ComparisonTerm.GE, since);
        return new AndTerm(linkedInTerm, receivedSince);
    }

    private static final class EmailImportAccumulator {
        private final int emailsScanned;
        private int emailsProcessed;
        private int emailsSkipped;
        private int emailsFailed;
        private int jobsExtracted;
        private int jobsSaved;

        private EmailImportAccumulator(int emailsScanned) {
            this.emailsScanned = emailsScanned;
        }

        private void incrementProcessed() {
            emailsProcessed++;
        }

        private void incrementSkipped() {
            emailsSkipped++;
        }

        private void incrementFailed() {
            emailsFailed++;
        }

        private void addExtractedJobs(int count) {
            jobsExtracted += count;
        }

        private void addSavedJobs(int count) {
            jobsSaved += count;
        }

        private EmailParseResult toResult() {
            return new EmailParseResult(
                    emailsScanned,
                    emailsProcessed,
                    emailsSkipped,
                    emailsFailed,
                    jobsExtracted,
                    jobsSaved
            );
        }
    }
}
