package com.alejandro.botjobhunter.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "companies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    private String name;
    private String website;
    private String industry;
    private String location;

    @Column(columnDefinition = "Text")
    private String description;

}
