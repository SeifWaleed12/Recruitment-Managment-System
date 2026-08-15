package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.data.entity.SkillEntity;
import com.banquemisr.recruitment.data.repo.SkillRepo;
import com.banquemisr.recruitment.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SkillService {
    private final SkillRepo skillRepo;


    public SkillEntity findOrCreate(String name){
        return skillRepo.findByNameIgnoreCase(name)
                .orElseGet(() -> skillRepo.save(SkillEntity.builder().name(name).build()));
    }

    public SkillEntity getSkillEntityById(String id) {
        return skillRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with ID: " + id));
    }
}
