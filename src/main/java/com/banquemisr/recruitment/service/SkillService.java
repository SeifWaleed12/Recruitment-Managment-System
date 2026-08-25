package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.data.entity.SkillEntity;
import com.banquemisr.recruitment.data.repo.SkillRepo;
import com.banquemisr.recruitment.exception.ResourceNotFoundException;
import com.banquemisr.recruitment.mapper.SkillMapper;
import com.banquemisr.recruitment.web.DTOs.request.SkillRequest;
import com.banquemisr.recruitment.web.DTOs.respond.SkillRespond;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SkillService {
    private final SkillRepo skillRepo;
    private final SkillMapper skillMapper;

    public List<SkillRespond> getAllSkills(String query) {
        List<SkillEntity> entities;
        if (query != null && !query.isBlank()) {
            entities = skillRepo.findByNameContainingIgnoreCase(query.trim());
        } else {
            entities = skillRepo.findAll();
        }
        return entities.stream().map(skillMapper::toRespond).toList();
    }

    public SkillRespond getSkillById(String id) {
        SkillEntity entity = getSkillEntityById(id);
        return skillMapper.toRespond(entity);
    }

    @Transactional
    public SkillRespond createSkill(SkillRequest request) {
        SkillEntity entity = findOrCreate(request.getName().trim());
        return skillMapper.toRespond(entity);
    }

    @Transactional
    public void deleteSkill(String id) {
        SkillEntity entity = getSkillEntityById(id);
        skillRepo.delete(entity);
    }

    public SkillEntity findOrCreate(String name){
        return skillRepo.findByNameIgnoreCase(name)
                .orElseGet(() -> skillRepo.save(SkillEntity.builder().name(name).build()));
    }

    public SkillEntity getSkillEntityById(String id) {
        return skillRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with ID: " + id));
    }
}
