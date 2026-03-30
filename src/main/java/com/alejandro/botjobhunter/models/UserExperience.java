package com.alejandro.botjobhunter.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name="user_experience")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder


public class UserExperience
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    private String jobTitle;
    private  String companyName;
    private LocalDate startDate;
    private LocalDate endDate;


    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String achievements;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

}
