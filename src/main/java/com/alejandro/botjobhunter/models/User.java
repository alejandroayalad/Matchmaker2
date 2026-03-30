package com.alejandro.botjobhunter.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;
    private String email;
    private String phone;

    private String linkedinURL;
    private String githubURL;


    @Column(columnDefinition = "TEXT")
    private String professionalSummary;

}
