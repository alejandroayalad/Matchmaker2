package com.alejandro.botjobhunter.dto;

import java.util.List;

public record EmailJobResultDTO(
        String title,
        String companyName,
        String location,
        String url,
        String summary,
        List<String> insights
) {
    public EmailJobResultDTO(String title, String companyName, String location, String url) {
        this(title, companyName, location, url, null, List.of());
    }

    public EmailJobResultDTO {
        insights = insights == null ? List.of() : List.copyOf(insights);
    }
}
