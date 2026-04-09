package com.alejandro.botjobhunter.controller;

import com.alejandro.botjobhunter.dto.ScoreCard;
import com.alejandro.botjobhunter.models.Job;
import com.alejandro.botjobhunter.repository.JobRepository;
import com.alejandro.botjobhunter.service.matching.JobScoringService;
import com.alejandro.botjobhunter.service.scrapper.ScraperOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobRepository jobRepository;
    private final ScraperOrchestrator jobScrapingOrchestrator;
    private final JobScoringService jobScoringService;

    @GetMapping
    public List<Job> getAll() {
        return jobRepository.findAll();
    }

    @GetMapping("/{id}")
    public Job getById(@PathVariable Long id) {
        return jobRepository.findById(id).orElseThrow();
    }

    @GetMapping("/scrape")
    public List<Job> scrapeJobs(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String location) {


        return jobScrapingOrchestrator.scrapeAndAggregateJobs(query, location);
    }

    @GetMapping("/matched")
    public List<ScoreCard> getMatchedJobs(@RequestParam Long userId) {
        try {
            return jobScoringService.getMatchedJobs(userId);
        } catch (NoSuchElementException exception) {
            throw new ResponseStatusException(NOT_FOUND, exception.getMessage(), exception);
        }
    }

    @PostMapping
    public Job create(@RequestBody Job job) {
        return jobRepository.save(job);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        jobRepository.deleteById(id);
    }
}
