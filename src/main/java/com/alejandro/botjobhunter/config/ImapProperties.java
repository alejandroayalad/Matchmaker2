package com.alejandro.botjobhunter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mail.imaps")
public record ImapProperties(
        String host,
        int port,
        String username,
        String password,
        boolean sslEnable,
        String folder
) {
}
