package com.banquemisr.recruitment.mapper;

import com.banquemisr.recruitment.data.entity.CandidateEntity;
import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.web.DTOs.request.CandidateRequest;
import com.banquemisr.recruitment.web.DTOs.respond.CandidateRespond;
import com.banquemisr.recruitment.web.DTOs.respond.SkillRespond;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CandidateMapper {

    private final TagMapper tagMapper;

    public CandidateEntity toEntity(CandidateRequest request, UserEntity creator) {
        if (request == null) return null;
        return CandidateEntity.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .yearsOfExperience(request.getYearsOfExperience())
                .createdBy(creator)
                .build();
    }

    public CandidateRespond toRespond(CandidateEntity entity) {
        if (entity == null) return null;

        String fullName = (entity.getFirstName() != null ? entity.getFirstName() : "") +
                " " + (entity.getLastName() != null ? entity.getLastName() : "");

        String creatorName = null;
        if (entity.getCreatedBy() != null) {
            creatorName = entity.getCreatedBy().getUserFname() + " " + entity.getCreatedBy().getUserLname();
        }

        return CandidateRespond.builder()
                .candidateId(entity.getCandidateId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .fullName(fullName.trim())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .yearsOfExperience(entity.getYearsOfExperience())
                .cvFilePath(entity.getCvFilePath())
                .cvOriginalFilename(entity.getCvOriginalFilename())
                .cvFileType(entity.getCvFileType())
                .createdByUserId(entity.getCreatedBy() != null ? entity.getCreatedBy().getUserId() : null)
                .createdByUserName(creatorName)
                .skills(entity.getSkills().stream()
                        .map(s -> SkillRespond.builder()
                                .id(s.getSkillId())
                                .name(s.getName())
                                .build())
                        .collect(Collectors.toSet()))
                .tags(entity.getTags().stream()
                        .map(tagMapper::toRespond)
                        .collect(Collectors.toSet()))
                .build();

    }
}
