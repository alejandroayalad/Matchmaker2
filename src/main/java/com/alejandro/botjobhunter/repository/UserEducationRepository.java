package com.alejandro.botjobhunter.repository;

import com.alejandro.botjobhunter.models.UserEducation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserEducationRepository extends JpaRepository<UserEducation, Long> {
}