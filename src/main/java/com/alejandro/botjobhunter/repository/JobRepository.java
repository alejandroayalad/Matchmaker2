package com.alejandro.botjobhunter.repository;

import com.alejandro.botjobhunter.models.Job;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {
    boolean existsByTitleIgnoreCaseAndCompany_NameIgnoreCaseAndUrlApplication(
            String title,
            String companyName,
            String urlApplication
    );
}
