package com.alejandro.botjobhunter.dto;

import jakarta.validation.constraints.NotBlank;

public record UrlImportRequest(
        @NotBlank String url
) {
}
