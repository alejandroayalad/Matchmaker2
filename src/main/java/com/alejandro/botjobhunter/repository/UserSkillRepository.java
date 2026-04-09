package com.alejandro.botjobhunter.repository;

import com.alejandro.botjobhunter.models.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {
    List<UserSkill> findAllByUser_Id(Long userId);
}
