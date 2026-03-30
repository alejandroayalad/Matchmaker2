package com.alejandro.botjobhunter.service.scrapper;

import com.alejandro.botjobhunter.models.Job;
import com.alejandro.botjobhunter.service.JobDeduplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScraperOrchestrator {

    private final List<JobScrapper> jobScrappers;
    private final JobDeduplicationService jobDeduplicationService;

    public List<Job> scrapeAndAggregateJobs(String query, String location) {
        List<Job> allScrapedJobs = new ArrayList<>();

        for (JobScrapper scrapper : jobScrappers) {
            try {
                List<Job> jobs = scrapper.searchJobs(query, location);
                if (jobs != null) {
                    allScrapedJobs.addAll(jobs);
                }
            } catch (Exception e) {
                System.err.println("Scraper failed (" + scrapper.getClass().getSimpleName() + "): " + e.getMessage());
            }
        }

        return jobDeduplicationService.deduplicate(allScrapedJobs);
    }
}