package com.alejandro.botjobhunter.dto;

import com.alejandro.botjobhunter.models.Job;

import java.util.List;

public record EmailParseResult(
        int emailsScanned,
        int emailsProcessed,
        int emailsSkipped,
        int emailsFailed,
        int jobsExtracted,
        int jobsSaved,
        List<EmailJobResultDTO> extractedJobs,
        List<Job> savedJobs
) {
    public EmailParseResult(int emailsScanned, int emailsProcessed, int emailsSkipped, int emailsFailed, int jobsExtracted, int jobsSaved) {
        this(emailsScanned, emailsProcessed, emailsSkipped, emailsFailed, jobsExtracted, jobsSaved, List.of(), List.of());
    }

    public EmailParseResult {
        extractedJobs = extractedJobs == null ? List.of() : List.copyOf(extractedJobs);
        savedJobs = savedJobs == null ? List.of() : List.copyOf(savedJobs);
    }
}
