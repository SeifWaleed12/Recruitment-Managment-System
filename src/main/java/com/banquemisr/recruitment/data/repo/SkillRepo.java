package com.banquemisr.recruitment.data.repo;

import com.banquemisr.recruitment.data.entity.SkillEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SkillRepo extends JpaRepository<SkillEntity,String> {
    Optional<SkillEntity> findByNameIgnoreCase(String name);
}
