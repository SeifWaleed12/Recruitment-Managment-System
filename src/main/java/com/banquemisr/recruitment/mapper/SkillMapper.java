package com.banquemisr.recruitment.mapper;

import com.banquemisr.recruitment.data.entity.SkillEntity;
import com.banquemisr.recruitment.web.DTOs.request.SkillRequest;
import com.banquemisr.recruitment.web.DTOs.respond.SkillRespond;
import org.springframework.stereotype.Component;

@Component
public class SkillMapper {

    public SkillEntity toEntity(SkillRequest request) {
        if (request == null) return null;
        return SkillEntity.builder()
                .name(request.getName())
                .build();
    }

    public SkillRespond toRespond(SkillEntity entity) {
        if (entity == null) return null;
        return SkillRespond.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }
}
