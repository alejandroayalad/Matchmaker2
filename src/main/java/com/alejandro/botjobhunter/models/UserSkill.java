package com.alejandro.botjobhunter.models;

import com.alejandro.botjobhunter.models.enums.ProficiencyLevel;
import com.alejandro.botjobhunter.models.enums.SkillCategory;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users_skills")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder


public class UserSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    private String skillName;

    @Enumerated(EnumType.STRING)
    private ProficiencyLevel proficiencyLevel;

    private Integer yearsOfExperience;

    @Enumerated (EnumType.STRING)
    private SkillCategory skillCategory;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
