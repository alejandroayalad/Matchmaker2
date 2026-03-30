package com.alejandro.botjobhunter.models;

import com.alejandro.botjobhunter.models.enums.ExperienceLevel;
import com.alejandro.botjobhunter.models.enums.JobType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    private String title;
    @Column(columnDefinition = "Text")
    private String description;
    @Column(columnDefinition = "Text")
    private String requirements;

    private String urlApplication;
    private String source;
    private String location;

    private String salary;

    @Enumerated(EnumType.STRING)
    private JobType jobType;

    @Enumerated (EnumType.STRING)
    private ExperienceLevel  experienceLevel;

    private String skills;
    private Integer applicationCount;
    private String recruiterName;
    private String recruiterLinkedin;

    private LocalDateTime scrappedAt;
    private Boolean active;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;
}
