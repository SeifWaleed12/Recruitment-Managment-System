package com.banquemisr.recruitment.mapper;

import com.banquemisr.recruitment.data.entity.JobEntity;
import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.web.DTOs.request.JobRequest;
import com.banquemisr.recruitment.web.DTOs.respond.JobRespond;
import org.springframework.stereotype.Component;

@Component
public class JobMapper {

    public JobEntity toEntity(JobRequest request, UserEntity creator) {
        if (request == null) return null;
        return JobEntity.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .department(request.getDepartment())
                .location(request.getLocation())
                .status(request.getStatus())
                .createdBy(creator)
                .build();
    }

    public JobRespond toRespond(JobEntity entity) {
        if (entity == null) return null;
        return JobRespond.builder()
                .jobId(entity.getJobId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .department(entity.getDepartment())
                .location(entity.getLocation())
                .status(entity.getStatus())
                .createdByUserId(entity.getCreatedBy() != null ? entity.getCreatedBy().getUserId() : null)
                .build();
    }
}
