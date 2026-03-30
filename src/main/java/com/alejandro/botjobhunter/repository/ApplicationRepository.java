package com.alejandro.botjobhunter.repository;

import com.alejandro.botjobhunter.models.Application;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

}
