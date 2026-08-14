package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.data.entity.SkillEntity;
import com.banquemisr.recruitment.data.repo.SkillRepo;

public class SkillService {
    private final SkillRepo skillRepo;

    public SkillService(SkillRepo skillRepo){
        this.skillRepo=skillRepo;
    }

    public SkillEntity findOrCreate(String name){
        return skillRepo.findByNameIgnoreCase(name)
                .orElseGet(() -> skillRepo.save(SkillEntity.builder().name(name).build()));
    }
}
