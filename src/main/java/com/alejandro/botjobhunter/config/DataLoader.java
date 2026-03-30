package com.alejandro.botjobhunter.config;

import com.alejandro.botjobhunter.models.*;
import com.alejandro.botjobhunter.models.enums.*;
import com.alejandro.botjobhunter.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserSkillRepository userSkillRepository;
    private final UserExperienceRepository userExperienceRepository;
    private final UserEducationRepository userEducationRepository;

    @Override
    public void run(String... args) {

        User alejandro = userRepository.save(User.builder()
                .firstName("Alejandro")
                .lastName("Ayalas")
                .email("your-email@example.com")
                .phone("your-phone")
                .professionalSummary("Full-stack developer specializing in Node.js and TypeScript with production experience building SaaS platforms for Mexican fiscal compliance.")
                .linkedinURL("https://linkedin.com/in/your-profile")
                .githubURL("https://github.com/your-profile")
                .build());


        userSkillRepository.saveAll(java.util.List.of(
                UserSkill.builder().user(alejandro).skillName("TypeScript").proficiencyLevel(ProficiencyLevel.ADVANCED).yearsOfExperience(3).skillCategory(SkillCategory.LANGUAGE).build(),
                UserSkill.builder().user(alejandro).skillName("Node.js").proficiencyLevel(ProficiencyLevel.ADVANCED).yearsOfExperience(3).skillCategory(SkillCategory.FRAMEWORK).build(),
                UserSkill.builder().user(alejandro).skillName("React").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).yearsOfExperience(2).skillCategory(SkillCategory.FRAMEWORK).build(),
                UserSkill.builder().user(alejandro).skillName("Java").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).yearsOfExperience(2).skillCategory(SkillCategory.LANGUAGE).build(),
                UserSkill.builder().user(alejandro).skillName("Spring Boot").proficiencyLevel(ProficiencyLevel.BEGINNER).yearsOfExperience(1).skillCategory(SkillCategory.FRAMEWORK).build(),
                UserSkill.builder().user(alejandro).skillName("PostgreSQL").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).yearsOfExperience(2).skillCategory(SkillCategory.DATABASE).build(),
                UserSkill.builder().user(alejandro).skillName("Docker").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).yearsOfExperience(2).skillCategory(SkillCategory.DEVOPS).build(),
                UserSkill.builder().user(alejandro).skillName("Redis").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).yearsOfExperience(2).skillCategory(SkillCategory.DATABASE).build(),
                UserSkill.builder().user(alejandro).skillName("C#").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).yearsOfExperience(1).skillCategory(SkillCategory.LANGUAGE).build(),
                UserSkill.builder().user(alejandro).skillName("Git").proficiencyLevel(ProficiencyLevel.ADVANCED).yearsOfExperience(3).skillCategory(SkillCategory.TOOL).build()
        ));


        userExperienceRepository.saveAll(java.util.List.of(
                UserExperience.builder()
                        .user(alejandro)
                        .jobTitle("Full-Stack Developer & Founder")
                        .companyName("FactuApp")
                        .startDate(java.time.LocalDate.of(2023, 1, 1))
                        .description("Production SaaS platform for Mexican fiscal compliance")
                        .achievements("Built SAT/CFDI integration, FIEL/CIEC authentication, Bull/BullMQ queue architecture, AES-256 encryption, JWT/RBAC auth, Docker deployment, CI/CD with GitHub Actions")
                        .build(),
                UserExperience.builder()
                        .user(alejandro)
                        .jobTitle("Developer")
                        .companyName("Zavala Solutions")
                        .description("Java and Salesforce development")
                        .build()
        ));


        userEducationRepository.save(UserEducation.builder()
                .user(alejandro)
                .institution("Universidad Modelo")
                .degree("Software Engineering")
                .field("Software Engineering")
                .status("PASANTE")
                .build());

        System.out.println(">>> Profile seeded: " + alejandro.getFirstName());
    }
}