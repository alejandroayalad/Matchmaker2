package com.alejandro.botjobhunter.repository;

import com.alejandro.botjobhunter.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
