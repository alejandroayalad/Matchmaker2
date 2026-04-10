package com.alejandro.botjobhunter.dto;

import java.time.LocalDateTime;

public record PipelineResult(
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        int jobsScraped,
        int newJobsSaved,
        int emailsScanned,
        int emailJobsSaved,
        int totalActiveJobs,
        int jobsScored,
        int highScoreMatches,
        int topScore,
        String status
) {
}