package com.alejandro.botjobhunter.service.pipeline;

import com.alejandro.botjobhunter.dto.EmailParseResult;
import com.alejandro.botjobhunter.dto.PipelineResult;
import com.alejandro.botjobhunter.dto.ScoreCard;
import com.alejandro.botjobhunter.models.Job;
import com.alejandro.botjobhunter.models.User;
import com.alejandro.botjobhunter.repository.JobRepository;
import com.alejandro.botjobhunter.repository.UserRepository;
import com.alejandro.botjobhunter.service.JobDeduplicationService;
import com.alejandro.botjobhunter.service.email.EmailOrchestrator;
import com.alejandro.botjobhunter.service.matching.JobScoringService;
import com.alejandro.botjobhunter.service.scrapper.ScraperOrchestrator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class PipelineOrchestrator {

    private static final int HIGH_SCORE_THRESHOLD = 30;

    private final ScraperOrchestrator scraperOrchestrator;
    private final ObjectProvider<EmailOrchestrator> emailOrchestratorProvider;
    private final JobDeduplicationService jobDeduplicationService;
    private final JobScoringService jobScoringService;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    public PipelineOrchestrator(
            ScraperOrchestrator scraperOrchestrator,
            ObjectProvider<EmailOrchestrator> emailOrchestratorProvider,
            JobDeduplicationService jobDeduplicationService,
            JobScoringService jobScoringService,
            JobRepository jobRepository,
            UserRepository userRepository
    ) {
        this.scraperOrchestrator = scraperOrchestrator;
        this.emailOrchestratorProvider = emailOrchestratorProvider;
        this.jobDeduplicationService = jobDeduplicationService;
        this.jobScoringService = jobScoringService;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
    }

    public PipelineResult run() {
        LocalDateTime startedAt = LocalDateTime.now();

        // Step 1: Scrape all sources
        ScrapeStepResult scrapeResult = executeScrapeStep();

        // Step 2: Import LinkedIn emails (if mail is enabled)
        EmailStepResult emailResult = executeEmailImportStep();

        // Step 3: Score all active jobs against the first user profile
        ScoreStepResult scoreResult = executeScoreStep();

        LocalDateTime finishedAt = LocalDateTime.now();

        PipelineResult result = new PipelineResult(
                startedAt,
                finishedAt,
                scrapeResult.jobsScraped(),
                scrapeResult.newJobsSaved(),
                emailResult.emailsScanned(),
                emailResult.jobsSaved(),
                scoreResult.totalActiveJobs(),
                scoreResult.jobsScored(),
                scoreResult.highScoreMatches(),
                scoreResult.topScore(),
                "COMPLETED"
        );

        logSummary(result);
        return result;
    }

    private ScrapeStepResult executeScrapeStep() {
        try {
            List<Job> scrapedJobs = scraperOrchestrator.scrapeAndAggregateJobs(null, null);
            int jobsScraped = scrapedJobs.size();
            int newJobsSaved = persistNewJobs(scrapedJobs);

            System.out.println(">>> Pipeline [SCRAPE]: scraped " + jobsScraped
                    + " jobs, saved " + newJobsSaved + " new.");
            return new ScrapeStepResult(jobsScraped, newJobsSaved);
        } catch (RuntimeException exception) {
            System.err.println(">>> Pipeline [SCRAPE] failed: " + exception.getMessage());
            return new ScrapeStepResult(0, 0);
        }
    }

    private EmailStepResult executeEmailImportStep() {
        EmailOrchestrator emailOrchestrator = emailOrchestratorProvider.getIfAvailable();
        if (emailOrchestrator == null) {
            System.out.println(">>> Pipeline [EMAIL]: skipped (mail not enabled).");
            return new EmailStepResult(0, 0);
        }

        try {
            EmailParseResult emailResult = emailOrchestrator.importLinkedInEmails(null);

            System.out.println(">>> Pipeline [EMAIL]: scanned " + emailResult.emailsScanned()
                    + " emails, saved " + emailResult.jobsSaved() + " jobs.");
            return new EmailStepResult(emailResult.emailsScanned(), emailResult.jobsSaved());
        } catch (Exception exception) {
            System.err.println(">>> Pipeline [EMAIL] failed: " + exception.getMessage());
            return new EmailStepResult(0, 0);
        }
    }

    private ScoreStepResult executeScoreStep() {
        User user = userRepository.findAll().stream().findFirst().orElse(null);
        if (user == null) {
            System.out.println(">>> Pipeline [SCORE]: skipped (no user profile found).");
            return new ScoreStepResult(0, 0, 0, 0);
        }

        try {
            List<ScoreCard> scoreCards = jobScoringService.getMatchedJobs(user.getId());
            int totalActive = scoreCards.size();
            int highScoreMatches = (int) scoreCards.stream()
                    .filter(card -> card.totalScore() >= HIGH_SCORE_THRESHOLD)
                    .count();
            int topScore = scoreCards.stream()
                    .mapToInt(ScoreCard::totalScore)
                    .max()
                    .orElse(0);

            System.out.println(">>> Pipeline [SCORE]: scored " + totalActive
                    + " jobs, " + highScoreMatches + " above threshold ("
                    + HIGH_SCORE_THRESHOLD + "), top score: " + topScore + ".");
            return new ScoreStepResult(totalActive, totalActive, highScoreMatches, topScore);
        } catch (RuntimeException exception) {
            System.err.println(">>> Pipeline [SCORE] failed: " + exception.getMessage());
            return new ScoreStepResult(0, 0, 0, 0);
        }
    }

    private int persistNewJobs(List<Job> jobs) {
        List<Job> uniqueJobs = jobDeduplicationService.deduplicate(jobs);
        int savedCount = 0;

        for (Job job : uniqueJobs) {
            String companyName = job.getCompany() != null
                    ? job.getCompany().getName()
                    : "";
            String title = Objects.requireNonNullElse(job.getTitle(), "");
            String url = Objects.requireNonNullElse(job.getUrlApplication(), "");

            if (jobRepository.existsByTitleIgnoreCaseAndCompany_NameIgnoreCaseAndUrlApplication(
                    title, companyName, url)) {
                continue;
            }

            job.setScrappedAt(LocalDateTime.now());
            job.setActive(true);
            jobRepository.save(job);
            savedCount++;
        }

        return savedCount;
    }

    private void logSummary(PipelineResult result) {
        System.out.println("=".repeat(60));
        System.out.println(">>> PIPELINE RUN COMPLETE");
        System.out.println("    Started:          " + result.startedAt());
        System.out.println("    Finished:         " + result.finishedAt());
        System.out.println("    Jobs scraped:     " + result.jobsScraped());
        System.out.println("    New jobs saved:   " + result.newJobsSaved());
        System.out.println("    Emails scanned:   " + result.emailsScanned());
        System.out.println("    Email jobs saved: " + result.emailJobsSaved());
        System.out.println("    Jobs scored:      " + result.jobsScored());
        System.out.println("    High matches:     " + result.highScoreMatches()
                + " (threshold: " + HIGH_SCORE_THRESHOLD + ")");
        System.out.println("    Top score:        " + result.topScore());
        System.out.println("=".repeat(60));
    }

    private record ScrapeStepResult(int jobsScraped, int newJobsSaved) {
    }

    private record EmailStepResult(int emailsScanned, int jobsSaved) {
    }

    private record ScoreStepResult(int totalActiveJobs, int jobsScored, int highScoreMatches, int topScore) {
    }
}