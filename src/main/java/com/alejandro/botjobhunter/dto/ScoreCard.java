package com.alejandro.botjobhunter.dto;

import java.util.List;

public record ScoreCard(
        Long jobId,
        int totalScore,
        List<String> skillMatches,
        boolean seniorityMatch,
        boolean modalityMatch,
        List<String> negativeSignals,
        List<String> matchBreakdown
) {
}
