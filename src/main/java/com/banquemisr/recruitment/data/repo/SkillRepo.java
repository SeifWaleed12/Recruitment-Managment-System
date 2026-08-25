package com.banquemisr.recruitment.data.repo;

import com.banquemisr.recruitment.data.entity.SkillEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SkillRepo extends JpaRepository<SkillEntity, String> {

    Optional<SkillEntity> findByNameIgnoreCase(String name);

    List<SkillEntity> findByNameContainingIgnoreCase(String name);

    List<SkillEntity> findByNameIn(Collection<String> names);
}
