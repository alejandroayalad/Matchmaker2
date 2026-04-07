package com.alejandro.botjobhunter.repository;

import com.alejandro.botjobhunter.models.EmailProcessingLog;
import com.alejandro.botjobhunter.models.enums.EmailStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailProcessingLogRepository extends JpaRepository<EmailProcessingLog, Long> {
    boolean existsByMessageId(String messageId);
    Page<EmailProcessingLog> findAllByStatus(EmailStatus status, Pageable pageable);
}
