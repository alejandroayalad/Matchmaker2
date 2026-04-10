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

        User alejandro = userRepository.save(User.builder()
                .firstName("Alejandro")
                .lastName("Ayala Díaz")
                .email("alejandroayaladiaz00@gmail.com")
                .phone("+52 999 322 1312")
                .professionalSummary(
                        "Backend developer with 3+ years of experience building production systems "
                                + "in Node.js/TypeScript, Java, and C#. Strong foundation in API design, "
                                + "queue-based architectures, Docker deployments, and CI/CD pipelines. "
                                + "Experienced working asynchronously and independently on complex, "
                                + "deadline-driven projects.")
                .linkedinURL("https://linkedin.com/in/alejandro-ayala")
                .githubURL("https://github.com/alejandroayalad")
                .build());


        userSkillRepository.saveAll(java.util.List.of(

                UserSkill.builder().user(alejandro).skillName("TypeScript").proficiencyLevel(ProficiencyLevel.ADVANCED).matchWeight(12).yearsOfExperience(3).skillCategory(SkillCategory.LANGUAGE).build(),
                UserSkill.builder().user(alejandro).skillName("JavaScript").proficiencyLevel(ProficiencyLevel.ADVANCED).matchWeight(10).yearsOfExperience(4).skillCategory(SkillCategory.LANGUAGE).build(),
                UserSkill.builder().user(alejandro).skillName("Java").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(7).yearsOfExperience(2).skillCategory(SkillCategory.LANGUAGE).build(),
                UserSkill.builder().user(alejandro).skillName("C#").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(4).yearsOfExperience(1).skillCategory(SkillCategory.LANGUAGE).build(),
                UserSkill.builder().user(alejandro).skillName("SQL").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(6).yearsOfExperience(3).skillCategory(SkillCategory.LANGUAGE).build(),


                UserSkill.builder().user(alejandro).skillName("Node.js").proficiencyLevel(ProficiencyLevel.ADVANCED).matchWeight(12).yearsOfExperience(3).skillCategory(SkillCategory.FRAMEWORK).build(),
                UserSkill.builder().user(alejandro).skillName("Express").proficiencyLevel(ProficiencyLevel.ADVANCED).matchWeight(8).yearsOfExperience(3).skillCategory(SkillCategory.FRAMEWORK).build(),
                UserSkill.builder().user(alejandro).skillName("Spring Boot").proficiencyLevel(ProficiencyLevel.BEGINNER).matchWeight(5).yearsOfExperience(1).skillCategory(SkillCategory.FRAMEWORK).build(),
                UserSkill.builder().user(alejandro).skillName("Prisma").proficiencyLevel(ProficiencyLevel.ADVANCED).matchWeight(7).yearsOfExperience(2).skillCategory(SkillCategory.FRAMEWORK).build(),
                UserSkill.builder().user(alejandro).skillName("REST APIs").proficiencyLevel(ProficiencyLevel.ADVANCED).matchWeight(8).yearsOfExperience(3).skillCategory(SkillCategory.FRAMEWORK).build(),
                UserSkill.builder().user(alejandro).skillName("Bull/BullMQ").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(5).yearsOfExperience(2).skillCategory(SkillCategory.FRAMEWORK).build(),
                UserSkill.builder().user(alejandro).skillName("Salesforce/Apex").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(3).yearsOfExperience(1).skillCategory(SkillCategory.FRAMEWORK).build(),


                UserSkill.builder().user(alejandro).skillName("React").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(6).yearsOfExperience(2).skillCategory(SkillCategory.FRAMEWORK).build(),
                UserSkill.builder().user(alejandro).skillName("HTML").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(3).yearsOfExperience(3).skillCategory(SkillCategory.FRAMEWORK).build(),
                UserSkill.builder().user(alejandro).skillName("CSS").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(3).yearsOfExperience(3).skillCategory(SkillCategory.FRAMEWORK).build(),


                UserSkill.builder().user(alejandro).skillName("PostgreSQL").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(7).yearsOfExperience(3).skillCategory(SkillCategory.DATABASE).build(),
                UserSkill.builder().user(alejandro).skillName("Redis").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(5).yearsOfExperience(2).skillCategory(SkillCategory.DATABASE).build(),


                UserSkill.builder().user(alejandro).skillName("Docker").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(6).yearsOfExperience(2).skillCategory(SkillCategory.DEVOPS).build(),
                UserSkill.builder().user(alejandro).skillName("Docker Compose").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(4).yearsOfExperience(2).skillCategory(SkillCategory.DEVOPS).build(),
                UserSkill.builder().user(alejandro).skillName("AWS").proficiencyLevel(ProficiencyLevel.BEGINNER).matchWeight(5).yearsOfExperience(1).skillCategory(SkillCategory.DEVOPS).build(),
                UserSkill.builder().user(alejandro).skillName("GitHub Actions").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(4).yearsOfExperience(2).skillCategory(SkillCategory.DEVOPS).build(),
                UserSkill.builder().user(alejandro).skillName("Nginx").proficiencyLevel(ProficiencyLevel.BEGINNER).matchWeight(3).yearsOfExperience(1).skillCategory(SkillCategory.DEVOPS).build(),
                UserSkill.builder().user(alejandro).skillName("Linux").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(3).yearsOfExperience(2).skillCategory(SkillCategory.DEVOPS).build(),
                UserSkill.builder().user(alejandro).skillName("CI/CD").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(4).yearsOfExperience(2).skillCategory(SkillCategory.DEVOPS).build(),


                UserSkill.builder().user(alejandro).skillName("Git").proficiencyLevel(ProficiencyLevel.ADVANCED).matchWeight(4).yearsOfExperience(3).skillCategory(SkillCategory.TOOL).build(),
                UserSkill.builder().user(alejandro).skillName("Stripe API").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(3).yearsOfExperience(1).skillCategory(SkillCategory.TOOL).build(),
                UserSkill.builder().user(alejandro).skillName("Airtable API").proficiencyLevel(ProficiencyLevel.INTERMEDIATE).matchWeight(2).yearsOfExperience(1).skillCategory(SkillCategory.TOOL).build(),
                UserSkill.builder().user(alejandro).skillName("LLM integration").proficiencyLevel(ProficiencyLevel.BEGINNER).matchWeight(4).yearsOfExperience(1).skillCategory(SkillCategory.TOOL).build(),
                UserSkill.builder().user(alejandro).skillName("n8n").proficiencyLevel(ProficiencyLevel.BEGINNER).matchWeight(2).yearsOfExperience(1).skillCategory(SkillCategory.TOOL).build(),


                UserSkill.builder().user(alejandro).skillName("English").proficiencyLevel(ProficiencyLevel.ADVANCED).matchWeight(4).yearsOfExperience(10).skillCategory(SkillCategory.LANGUAGE).build(),
                UserSkill.builder().user(alejandro).skillName("Spanish").proficiencyLevel(ProficiencyLevel.ADVANCED).matchWeight(2).yearsOfExperience(25).skillCategory(SkillCategory.LANGUAGE).build()
        ));

        // =====================================================================
        // WORK EXPERIENCE — all roles from resume
        // =====================================================================

        userExperienceRepository.saveAll(java.util.List.of(
                UserExperience.builder()
                        .user(alejandro)
                        .jobTitle("Backend Developer")
                        .companyName("FactuApp (SaaS)")
                        .startDate(LocalDate.of(2025, 1, 1))
                        .description(
                                "Designed and built the backend for a multi-tenant SaaS platform handling "
                                        + "automated document processing, third-party API integration, and "
                                        + "role-based access control for accounting firms.")
                        .achievements(
                                "SAT/CFDI integration with FIEL/CIEC authentication, "
                                        + "AES-256-CBC encryption with dynamic salt, "
                                        + "Bull/BullMQ queue architecture with exponential backoff, "
                                        + "payment reconciliation engine with currency normalization, "
                                        + "JWT/RBAC shared access system with invite tokens, "
                                        + "Docker Compose deployment on AWS EC2 with GitHub Actions CI/CD")
                        .build(),

                UserExperience.builder()
                        .user(alejandro)
                        .jobTitle("Freelance Full Stack Developer")
                        .companyName("Various Clients")
                        .startDate(LocalDate.of(2024, 1, 1))
                        .description(
                                "Built production applications for multiple clients including a pet adoption "
                                        + "matching platform, an ERP-style payment collection system, and an "
                                        + "LLM-based appointment management agent.")
                        .achievements(
                                "Lofty App: matching algorithm scoring candidates against available animals, "
                                        + "ERP payment system: Airtable + Stripe integration with automated invoicing, "
                                        + "Barber shop agent: LLM-based natural language booking and rescheduling")
                        .build(),

                UserExperience.builder()
                        .user(alejandro)
                        .jobTitle("Marketing & Tech Assistant")
                        .companyName("Helix Pro Grip")
                        .startDate(LocalDate.of(2023, 1, 1))
                        .endDate(LocalDate.of(2025, 1, 1))
                        .description(
                                "Built landing pages and managed e-commerce storefronts using WordPress and Shopify. "
                                        + "Automated repetitive business processes through custom scripts.")
                        .achievements(
                                "High-conversion landing pages, Shopify storefront management, "
                                        + "custom automation scripts reducing manual data entry")
                        .build(),

                UserExperience.builder()
                        .user(alejandro)
                        .jobTitle("Developer")
                        .companyName("Zavala Solutions")
                        .startDate(LocalDate.of(2022, 6, 1))
                        .endDate(LocalDate.of(2023, 6, 1))
                        .description("Java and Salesforce/Apex development.")
                        .achievements("Production Salesforce/Apex development and Java backend work")
                        .build()
        ));

        // =====================================================================
        // EDUCATION
        // =====================================================================

        userEducationRepository.save(UserEducation.builder()
                .user(alejandro)
                .institution("Universidad Modelo")
                .degree("B.S. in Software Development & Technology")
                .field("Software Engineering")
                .startDate(LocalDate.of(2020, 8, 1))
                .endDate(LocalDate.of(2025, 6, 1))
                .status("COURSEWORK_COMPLETED")
                .build());

        // =====================================================================
        // JOB SEARCH PREFERENCES — broader to catch more relevant matches
        // =====================================================================

        jobSearchPreferencesRepository.save(
                JobSearchPreferences.builder()
                        .user(alejandro)
                        .targetSeniority(ExperienceLevel.MID)
                        .preferredModality(JobType.REMOTE)
                        .preferredLocation("Merida, Yucatan")
                        .minimumSalary("20000 MXN monthly")
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
                                "Tata Consultancy Services",
                                "Teleperformance",
                                "Wipro",
                                "Infosys"
                        ))
                        .build()
        );

        System.out.println(">>> Profile seeded: " + alejandro.getFirstName() + " " + alejandro.getLastName());
    }
}