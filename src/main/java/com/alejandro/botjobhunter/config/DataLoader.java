package com.alejandro.botjobhunter.config;

import com.alejandro.botjobhunter.models.*;
import com.alejandro.botjobhunter.models.enums.*;
import com.alejandro.botjobhunter.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserSkillRepository userSkillRepository;
    private final UserExperienceRepository userExperienceRepository;
    private final UserEducationRepository userEducationRepository;
    private final JobSearchPreferencesRepository jobSearchPreferencesRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            System.out.println(">>> Seed skipped: users already exist.");
            return;
        }

        User demoUser = userRepository.save(User.builder()
                .firstName("Demo")
                .lastName("User")
                .email("demo@example.com")
                .phone("+52 000 000 0000")
                .professionalSummary(
                        "Backend developer with experience building production systems "
                                + "in Node.js/TypeScript, Java, and C#. Strong foundation in API design, "
                                + "queue-based architectures, Docker deployments, and CI/CD pipelines. "
                                + "Comfortable working independently on deadline-driven projects.")
                .linkedinURL("https://linkedin.com/in/demo-user")
                .githubURL("https://github.com/demo-user")
                .build());

        userSkillRepository.saveAll(java.util.List.of(
                UserSkill.builder().user(demoUser).skillName("TypeScript").proficiencyLevel(ProficiencyLevel.ADVANCED).matchWeight(12).yearsOfExperience(3).skillCategory(SkillCategory.LANGUAGE).build(),
                UserSkill.builder().user(demoUser).skillName("JavaScript").proficiencyLevel(ProficiencyLevel.ADVANCED).matchWeight(10).yearsOfExperience(4).skillCategory(SkillCategory.LANGUAGE).build(),
                UserSkill.builder().user(demoUser).skillName("Java").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(7).yearsOfExperience(2).skillCategory(SkillCategory.LANGUAGE).build(),
                UserSkill.builder().user(demoUser).skillName("C#").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(4).yearsOfExperience(1).skillCategory(SkillCategory.LANGUAGE).build(),
                UserSkill.builder().user(demoUser).skillName("SQL").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(6).yearsOfExperience(3).skillCategory(SkillCategory.LANGUAGE).build(),

                UserSkill.builder().user(demoUser).skillName("Node.js").proficiencyLevel(ProficiencyLevel.ADVANCED).matchWeight(12).yearsOfExperience(3).skillCategory(SkillCategory.FRAMEWORK).build(),
                UserSkill.builder().user(demoUser).skillName("Express").proficiencyLevel(ProficiencyLevel.ADVANCED).matchWeight(8).yearsOfExperience(3).skillCategory(SkillCategory.FRAMEWORK).build(),
                UserSkill.builder().user(demoUser).skillName("Spring Boot").proficiencyLevel(ProficiencyLevel.BEGINNER).matchWeight(5).yearsOfExperience(1).skillCategory(SkillCategory.FRAMEWORK).build(),
                UserSkill.builder().user(demoUser).skillName("Prisma").proficiencyLevel(ProficiencyLevel.ADVANCED).matchWeight(7).yearsOfExperience(2).skillCategory(SkillCategory.FRAMEWORK).build(),
                UserSkill.builder().user(demoUser).skillName("REST APIs").proficiencyLevel(ProficiencyLevel.ADVANCED).matchWeight(8).yearsOfExperience(3).skillCategory(SkillCategory.FRAMEWORK).build(),
                UserSkill.builder().user(demoUser).skillName("Bull/BullMQ").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(5).yearsOfExperience(2).skillCategory(SkillCategory.FRAMEWORK).build(),
                UserSkill.builder().user(demoUser).skillName("Salesforce/Apex").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(3).yearsOfExperience(1).skillCategory(SkillCategory.FRAMEWORK).build(),

                UserSkill.builder().user(demoUser).skillName("React").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(6).yearsOfExperience(2).skillCategory(SkillCategory.FRAMEWORK).build(),
                UserSkill.builder().user(demoUser).skillName("HTML").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(3).yearsOfExperience(3).skillCategory(SkillCategory.FRAMEWORK).build(),
                UserSkill.builder().user(demoUser).skillName("CSS").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(3).yearsOfExperience(3).skillCategory(SkillCategory.FRAMEWORK).build(),

                UserSkill.builder().user(demoUser).skillName("PostgreSQL").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(7).yearsOfExperience(3).skillCategory(SkillCategory.DATABASE).build(),
                UserSkill.builder().user(demoUser).skillName("Redis").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(5).yearsOfExperience(2).skillCategory(SkillCategory.DATABASE).build(),

                UserSkill.builder().user(demoUser).skillName("Docker").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(6).yearsOfExperience(2).skillCategory(SkillCategory.DEVOPS).build(),
                UserSkill.builder().user(demoUser).skillName("Docker Compose").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(4).yearsOfExperience(2).skillCategory(SkillCategory.DEVOPS).build(),
                UserSkill.builder().user(demoUser).skillName("AWS").proficiencyLevel(ProficiencyLevel.BEGINNER).matchWeight(5).yearsOfExperience(1).skillCategory(SkillCategory.DEVOPS).build(),
                UserSkill.builder().user(demoUser).skillName("GitHub Actions").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(4).yearsOfExperience(2).skillCategory(SkillCategory.DEVOPS).build(),
                UserSkill.builder().user(demoUser).skillName("Nginx").proficiencyLevel(ProficiencyLevel.BEGINNER).matchWeight(3).yearsOfExperience(1).skillCategory(SkillCategory.DEVOPS).build(),
                UserSkill.builder().user(demoUser).skillName("Linux").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(3).yearsOfExperience(2).skillCategory(SkillCategory.DEVOPS).build(),
                UserSkill.builder().user(demoUser).skillName("CI/CD").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(4).yearsOfExperience(2).skillCategory(SkillCategory.DEVOPS).build(),

                UserSkill.builder().user(demoUser).skillName("Git").proficiencyLevel(ProficiencyLevel.ADVANCED).matchWeight(4).yearsOfExperience(3).skillCategory(SkillCategory.TOOL).build(),
                UserSkill.builder().user(demoUser).skillName("Stripe API").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(3).yearsOfExperience(1).skillCategory(SkillCategory.TOOL).build(),
                UserSkill.builder().user(demoUser).skillName("Airtable API").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(2).yearsOfExperience(1).skillCategory(SkillCategory.TOOL).build(),
                UserSkill.builder().user(demoUser).skillName("LLM integration").proficiencyLevel(ProficiencyLevel.BEGINNER).matchWeight(4).yearsOfExperience(1).skillCategory(SkillCategory.TOOL).build(),
                UserSkill.builder().user(demoUser).skillName("n8n").proficiencyLevel(ProficiencyLevel.BEGINNER).matchWeight(2).yearsOfExperience(1).skillCategory(SkillCategory.TOOL).build(),

                UserSkill.builder().user(demoUser).skillName("English").proficiencyLevel(ProficiencyLevel.ADVANCED).matchWeight(4).yearsOfExperience(10).skillCategory(SkillCategory.LANGUAGE).build(),
                UserSkill.builder().user(demoUser).skillName("Spanish").proficiencyLevel(ProficiencyLevel.ADVANCED).matchWeight(2).yearsOfExperience(25).skillCategory(SkillCategory.LANGUAGE).build()
        ));

        userExperienceRepository.saveAll(java.util.List.of(
                UserExperience.builder()
                        .user(demoUser)
                        .jobTitle("Backend Developer")
                        .companyName("Compliance SaaS")
                        .startDate(LocalDate.of(2025, 1, 1))
                        .description(
                                "Designed and built the backend for a multi-tenant SaaS platform handling "
                                        + "automated document processing, third-party API integration, and "
                                        + "role-based access control.")
                        .achievements(
                                "Third-party compliance integrations, AES-256 encryption, "
                                        + "queue-based architecture with retry policies, "
                                        + "payment reconciliation, shared access controls, "
                                        + "and Docker-based deployment with CI/CD")
                        .build(),

                UserExperience.builder()
                        .user(demoUser)
                        .jobTitle("Freelance Full Stack Developer")
                        .companyName("Various Clients")
                        .startDate(LocalDate.of(2024, 1, 1))
                        .description(
                                "Built production applications for multiple clients including a matching platform, "
                                        + "a payment collection system, and a natural-language scheduling assistant.")
                        .achievements(
                                "Matching algorithm design, billing workflow automation, "
                                        + "and LLM-based booking and rescheduling flows")
                        .build(),

                UserExperience.builder()
                        .user(demoUser)
                        .jobTitle("Marketing & Tech Assistant")
                        .companyName("E-commerce Brand")
                        .startDate(LocalDate.of(2023, 1, 1))
                        .endDate(LocalDate.of(2025, 1, 1))
                        .description(
                                "Built landing pages and supported e-commerce storefront operations. "
                                        + "Automated repetitive business processes through custom scripts.")
                        .achievements(
                                "Landing page development, storefront support, "
                                        + "and internal automation for repetitive workflows")
                        .build(),

                UserExperience.builder()
                        .user(demoUser)
                        .jobTitle("Developer")
                        .companyName("Consulting Firm")
                        .startDate(LocalDate.of(2022, 6, 1))
                        .endDate(LocalDate.of(2023, 6, 1))
                        .description("Java and Salesforce/Apex development.")
                        .achievements("Business application development and backend support")
                        .build()
        ));

        userEducationRepository.save(UserEducation.builder()
                .user(demoUser)
                .institution("State University")
                .degree("B.S. in Software Development & Technology")
                .field("Software Engineering")
                .startDate(LocalDate.of(2020, 8, 1))
                .endDate(LocalDate.of(2025, 6, 1))
                .status("COURSEWORK_COMPLETED")
                .build());

        jobSearchPreferencesRepository.save(
                JobSearchPreferences.builder()
                        .user(demoUser)
                        .targetSeniority(ExperienceLevel.MID)
                        .preferredModality(JobType.REMOTE)
                        .preferredLocation("Remote")
                        .minimumSalary("Negotiable")
                        .positiveKeywords(java.util.List.of(
                                "typescript", "node.js", "nodejs", "backend", "remote",
                                "saas", "react", "aws", "docker", "api",
                                "rest", "postgresql", "redis", "llm", "ai",
                                "full-stack", "fullstack", "full stack",
                                "javascript", "prisma", "express"
                        ))
                        .negativeKeywords(java.util.List.of(
                                "salesforce", "php", "support", "qa",
                                "manual testing", "data entry"
                        ))
                        .targetRoleTitles(java.util.List.of(
                                "backend engineer", "backend developer",
                                "full-stack developer", "full-stack engineer",
                                "fullstack developer", "fullstack engineer",
                                "software engineer", "software developer",
                                "node.js developer", "nodejs developer",
                                "typescript developer",
                                "api developer", "api engineer",
                                "web developer"
                        ))
                        .blacklistedCompanies(java.util.List.of(
                                "Example Outsourcing Co.",
                                "Example Support Vendor"
                        ))
                        .build()
        );

        System.out.println(">>> Profile seeded: " + demoUser.getFirstName() + " " + demoUser.getLastName());
    }
}
