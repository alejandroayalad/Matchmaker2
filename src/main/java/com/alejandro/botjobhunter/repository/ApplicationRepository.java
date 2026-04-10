package com.alejandro.botjobhunter.repository;

import com.alejandro.botjobhunter.models.Application;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    @Override
    @EntityGraph(attributePaths = {"user", "job", "job.company"})
    List<Application> findAll();

    @Override
    @EntityGraph(attributePaths = {"user", "job", "job.company"})
    Optional<Application> findById(Long id);
}
