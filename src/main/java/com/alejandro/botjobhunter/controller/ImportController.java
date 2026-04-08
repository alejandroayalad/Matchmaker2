package com.alejandro.botjobhunter.controller;

import com.alejandro.botjobhunter.dto.EmailParseResult;
import com.alejandro.botjobhunter.dto.UrlImportRequest;
import com.alejandro.botjobhunter.models.EmailProcessingLog;
import com.alejandro.botjobhunter.models.Job;
import com.alejandro.botjobhunter.models.enums.EmailStatus;
import com.alejandro.botjobhunter.repository.EmailProcessingLogRepository;
import com.alejandro.botjobhunter.service.email.EmailOrchestrator;
import com.alejandro.botjobhunter.service.importer.UrlImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {
    private static final int MAX_PAGE_SIZE = 100;

    private final ObjectProvider<EmailOrchestrator> emailOrchestratorProvider;
    private final EmailProcessingLogRepository emailProcessingLogRepository;
    private final UrlImportService urlImportService;

    @PostMapping("/email")
    public EmailParseResult importLinkedInEmails(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime since
    ) {
        EmailOrchestrator emailOrchestrator = emailOrchestratorProvider.getIfAvailable();
        if (emailOrchestrator == null) {
            throw new ResponseStatusException(
                    SERVICE_UNAVAILABLE,
                    "Email import is disabled. Set MAIL_ENABLED=true to enable it."
            );
        }

        try {
            return emailOrchestrator.importLinkedInEmails(toDate(since));
        } catch (jakarta.mail.MessagingException exception) {
            throw new ResponseStatusException(BAD_GATEWAY, "Failed to import LinkedIn emails.", exception);
        }
    }

    @PostMapping("/url")
    public Job importJobFromUrl(@Valid @RequestBody UrlImportRequest request) {
        try {
            return urlImportService.importFromUrl(request.url());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(BAD_REQUEST, exception.getMessage(), exception);
        } catch (IllegalStateException exception) {
            if (isDuplicateImportFailure(exception)) {
                throw new ResponseStatusException(CONFLICT, exception.getMessage(), exception);
            }

            throw new ResponseStatusException(BAD_GATEWAY, "Failed to import job from URL.", exception);
        }
    }

    @GetMapping("/email/log")
    public Page<EmailProcessingLog> getEmailImportLog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) EmailStatus status
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "processedAt")
        );

        if (status != null) {
            return emailProcessingLogRepository.findAllByStatus(status, pageable);
        }

        return emailProcessingLogRepository.findAll(pageable);
    }

    private Date toDate(LocalDateTime value) {
        if (value == null) {
            return null;
        }

        return Date.from(value.atZone(ZoneId.systemDefault()).toInstant());
    }

    private boolean isDuplicateImportFailure(IllegalStateException exception) {
        return exception.getMessage() != null
                && exception.getMessage().toLowerCase().contains("already exists");
    }
}
