package com.banquemisr.recruitment.data.repo;

import com.banquemisr.recruitment.data.entity.TagEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepo extends JpaRepository<TagEntity, String> {
    Optional<TagEntity> findByNameIgnoreCase(String name);
}
