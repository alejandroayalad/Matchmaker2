package com.alejandro.botjobhunter.models;

import com.alejandro.botjobhunter.models.enums.EmailStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_processing_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailProcessingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String messageId;

    private String subject;
    private String sender;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    @Column(nullable = false)
    private Integer jobsExtracted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailStatus status;
}
