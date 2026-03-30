package com.alejandro.botjobhunter.repository;

import com.alejandro.botjobhunter.models.UserExperience;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserExperienceRepository extends JpaRepository<UserExperience, Long> {
}