package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.data.entity.ApplicationEntity;
import com.banquemisr.recruitment.data.entity.CandidateEntity;
import com.banquemisr.recruitment.data.entity.JobEntity;
import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.enums.ApplicationStatus;
import com.banquemisr.recruitment.data.repo.ApplicationRepo;
import com.banquemisr.recruitment.exception.DuplicateResourceException;
import com.banquemisr.recruitment.exception.ResourceNotFoundException;
import com.banquemisr.recruitment.mapper.ApplicationMapper;
import com.banquemisr.recruitment.web.DTOs.request.ApplicationRequest;
import com.banquemisr.recruitment.web.DTOs.respond.ApplicationRespond;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepo applicationRepo;
    private final CandidateService candidateService;
    private final JobService jobService;
    private final UserService userService;
    private final ApplicationMapper applicationMapper;

    @Transactional
    public ApplicationRespond createApplication(ApplicationRequest request) {
        // 1. Fetch Candidate Entity via CandidateService
        CandidateEntity candidate = candidateService.getCandidateEntityById(request.getCandidateId());
        // 2. Fetch Job Entity via JobService
        JobEntity job = jobService.getJobEntityById(request.getJobId());
        // 3. Prevent duplicate applications
        if (applicationRepo.existsByJobJobIdAndCandidateCandidateId(job.getJobId(), candidate.getCandidateId())) {
            throw new DuplicateResourceException("Candidate has already applied for this job!");
        }
        // 4. Fetch optional Recruiter User Entity via UserService
        UserEntity recruiter = null;
        if (request.getAssignedRecruiterId() != null && !request.getAssignedRecruiterId().isBlank()) {
            recruiter = userService.getUserEntityById(request.getAssignedRecruiterId());
        }
        if (request.getStatus() == null) {
            request.setStatus(ApplicationStatus.APPLIED);
        }
        ApplicationEntity entity = applicationMapper.toEntity(request, candidate, job, recruiter);
        ApplicationEntity savedEntity = applicationRepo.save(entity);
        return applicationMapper.toRespond(savedEntity);
    }

    @Transactional(readOnly = true)
    public ApplicationRespond getApplicationById(String applicationId) {
        ApplicationEntity entity = applicationRepo.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));
        return applicationMapper.toRespond(entity);
    }

    @Transactional(readOnly = true)
    public List<ApplicationRespond> getApplicationsByJobId(String jobId) {
        return applicationRepo.findByJobJobId(jobId).stream()
                .map(applicationMapper::toRespond)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationRespond> getApplicationsByCandidateId(String candidateId) {
        return applicationRepo.findByCandidateCandidateId(candidateId).stream()
                .map(applicationMapper::toRespond)
                .collect(Collectors.toList());
    }

    @Transactional
    public ApplicationRespond transitionStatus(String applicationId, ApplicationStatus newStatus){
        ApplicationEntity entity= applicationRepo.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));
        entity.transitionTo(newStatus);
        ApplicationEntity updatedEntity = applicationRepo.save(entity);
        return applicationMapper.toRespond(updatedEntity);
    }

    @Transactional
    public ApplicationRespond assignRecruiter(String applicationId, String recruiterUserId) {
        ApplicationEntity entity = applicationRepo.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));
        // Fetch Recruiter Entity via UserService
        UserEntity recruiter = userService.getUserEntityById(recruiterUserId);
        entity.setAssignedRecruiter(recruiter);
        ApplicationEntity updatedEntity = applicationRepo.save(entity);
        return applicationMapper.toRespond(updatedEntity);
    }

    @Transactional
    public void deleteApplication(String applicationId) {
        if (!applicationRepo.existsById(applicationId)) {
            throw new ResourceNotFoundException("Application not found with ID: " + applicationId);
        }
        applicationRepo.deleteById(applicationId);
    }
}
