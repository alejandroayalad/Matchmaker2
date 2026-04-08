package com.alejandro.botjobhunter.service.importer;


import com.alejandro.botjobhunter.dto.UrlImportResultDTO;
import com.alejandro.botjobhunter.models.enums.JobSource;

public interface UrlJobImporter {
    boolean canHandle(String url);
    JobSource getJobSource();
    UrlImportResultDTO importFromUrl(String url);
}
