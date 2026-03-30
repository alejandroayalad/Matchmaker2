package com.alejandro.botjobhunter.controller;

import com.alejandro.botjobhunter.models.Job;
import com.alejandro.botjobhunter.repository.JobRepository;
import com.alejandro.botjobhunter.service.ScraperOrchestrator;
import com.alejandro.botjobhunter.service.scrapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobRepository jobRepository;
    private final ScraperOrchestrator jobScrapingOrchestrator;

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

    @PostMapping
    public Job create(@RequestBody Job job) {
        return jobRepository.save(job);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        jobRepository.deleteById(id);
    }
}