package com.alejandro.botjobhunter.repository;

import com.alejandro.botjobhunter.models.JobSearchPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobSearchPreferencesRepository extends JpaRepository<JobSearchPreferences, Long> {
    Optional<JobSearchPreferences> findByUser_Id(Long userId);
}
