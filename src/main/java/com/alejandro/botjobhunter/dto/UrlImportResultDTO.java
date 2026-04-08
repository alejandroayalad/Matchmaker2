package com.alejandro.botjobhunter.dto;

public record UrlImportResultDTO(
        String title,
        String companyName,
        String location,
        String description,
        String salary,
        String url
) {
}
