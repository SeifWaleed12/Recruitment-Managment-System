package com.banquemisr.recruitment.mapper;

import com.banquemisr.recruitment.data.entity.ApplicationEntity;
import com.banquemisr.recruitment.data.entity.CandidateEntity;
import com.banquemisr.recruitment.data.entity.JobEntity;
import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.web.DTOs.request.ApplicationRequest;
import com.banquemisr.recruitment.web.DTOs.respond.ApplicationRespond;
import org.springframework.stereotype.Component;

@Component
public class ApplicationMapper {

    public ApplicationEntity toEntity(ApplicationRequest request, CandidateEntity candidate, JobEntity job, UserEntity recruiter) {
        if (request == null) return null;
        return ApplicationEntity.builder()
                .candidate(candidate)
                .job(job)
                .status(request.getStatus())
                .assignedRecruiter(recruiter)
                .build();
    }

    public ApplicationRespond toRespond(ApplicationEntity entity) {
        if (entity == null) return null;

        String recruiterName = null;
        if (entity.getAssignedRecruiter() != null) {
            recruiterName = entity.getAssignedRecruiter().getUserFname() + " " + entity.getAssignedRecruiter().getUserLname();
        }

        return ApplicationRespond.builder()
                .applicationId(entity.getApplicationId())
                .candidateId(entity.getCandidate() != null ? entity.getCandidate().getCandidateId() : null)
                .candidateFname(entity.getCandidate() != null ? entity.getCandidate().getFirstName() : null)
                .jobId(entity.getJob() != null ? entity.getJob().getJobId() : null)
                .jobTitle(entity.getJob() != null ? entity.getJob().getTitle() : null)
                .status(entity.getStatus())
                .assignedRecruiter(recruiterName)
                .build();
    }
}
