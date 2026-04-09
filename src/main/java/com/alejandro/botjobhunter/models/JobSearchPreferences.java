package com.alejandro.botjobhunter.models;

import com.alejandro.botjobhunter.models.enums.ExperienceLevel;
import com.alejandro.botjobhunter.models.enums.JobType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_search_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobSearchPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private ExperienceLevel targetSeniority;

    @Enumerated(EnumType.STRING)
    private JobType preferredModality;

    private String preferredLocation;
    private String minimumSalary;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "job_search_preferences_positive_keywords", joinColumns = @JoinColumn(name = "preferences_id"))
    @Column(name = "keyword")
    private List<String> positiveKeywords = new ArrayList<>();

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "job_search_preferences_negative_keywords", joinColumns = @JoinColumn(name = "preferences_id"))
    @Column(name = "keyword")
    private List<String> negativeKeywords = new ArrayList<>();

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "job_search_preferences_target_role_titles", joinColumns = @JoinColumn(name = "preferences_id"))
    @Column(name = "role_title")
    private List<String> targetRoleTitles = new ArrayList<>();

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "job_search_preferences_blacklisted_companies", joinColumns = @JoinColumn(name = "preferences_id"))
    @Column(name = "company_name")
    private List<String> blacklistedCompanies = new ArrayList<>();
}
