package com.alejandro.botjobhunter.repository;

import com.alejandro.botjobhunter.models.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByName(String company);
    Optional<Company> findByNameIgnoreCase(String company);
}
