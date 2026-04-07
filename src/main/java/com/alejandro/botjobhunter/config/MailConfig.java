package com.alejandro.botjobhunter.config;

import jakarta.mail.Session;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
@ConditionalOnProperty(prefix = "mail", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ImapProperties.class)
public class MailConfig {

    @Bean
    public Session mailSession(ImapProperties imapProperties) {
        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imaps");
        properties.put("mail.imaps.host", imapProperties.host());
        properties.put("mail.imaps.port", String.valueOf(imapProperties.port()));
        properties.put("mail.imaps.ssl.enable", String.valueOf(imapProperties.sslEnable()));
        return Session.getInstance(properties);
    }
}
