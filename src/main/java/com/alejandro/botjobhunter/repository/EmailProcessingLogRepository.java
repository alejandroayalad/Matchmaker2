package com.alejandro.botjobhunter.repository;

import com.alejandro.botjobhunter.models.EmailProcessingLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailProcessingLogRepository extends JpaRepository<EmailProcessingLog, Long> {
    boolean existsByMessageId(String messageId);
}
