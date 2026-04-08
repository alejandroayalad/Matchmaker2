package com.alejandro.botjobhunter.config;

import com.alejandro.botjobhunter.models.enums.EmailCleanupStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mail.cleanup")
public record EmailCleanupProperties(
        EmailCleanupStrategy strategy,
        String label
) {
}
