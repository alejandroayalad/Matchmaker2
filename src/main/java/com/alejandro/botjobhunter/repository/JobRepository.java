package com.alejandro.botjobhunter.repository;

import com.alejandro.botjobhunter.models.Job;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long>, JobRepositoryCustom {
    @Override
    @EntityGraph(attributePaths = "company")
    List<Job> findAll();

    @EntityGraph(attributePaths = "company")
    List<Job> findAllByOrderByActiveDescScrappedAtDesc();

    @Override
    @EntityGraph(attributePaths = "company")
    Optional<Job> findById(Long id);

    boolean existsByTitleIgnoreCaseAndCompany_NameIgnoreCaseAndUrlApplication(
            String title,
            String companyName,
            String urlApplication
    );
}
