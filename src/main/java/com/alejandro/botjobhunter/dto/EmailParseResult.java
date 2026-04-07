package com.alejandro.botjobhunter.dto;

public record EmailParseResult(
        int emailsScanned,
        int emailsProcessed,
        int emailsSkipped,
        int emailsFailed,
        int jobsExtracted,
        int jobsSaved
) {
}
