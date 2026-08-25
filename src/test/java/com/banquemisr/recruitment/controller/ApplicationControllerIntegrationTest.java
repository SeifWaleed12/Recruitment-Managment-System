package com.banquemisr.recruitment.controller;

import com.banquemisr.recruitment.base.BaseIntegrationTest;
import com.banquemisr.recruitment.data.entity.ApplicationEntity;
import com.banquemisr.recruitment.data.entity.CandidateEntity;
import com.banquemisr.recruitment.data.entity.JobEntity;
import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.enums.ApplicationStatus;
import com.banquemisr.recruitment.data.enums.JobStatus;
import com.banquemisr.recruitment.data.enums.Role;
import com.banquemisr.recruitment.data.repo.ApplicationRepo;
import com.banquemisr.recruitment.data.repo.CandidateRepo;
import com.banquemisr.recruitment.data.repo.JobRepo;
import com.banquemisr.recruitment.web.DTOs.request.ApplicationRequest;
import com.banquemisr.recruitment.web.DTOs.request.TransitionStatusRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApplicationControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ApplicationRepo applicationRepo;

    @Autowired
    private CandidateRepo candidateRepo;

    @Autowired
    private JobRepo jobRepo;

    @Test
    @WithMockUser(roles = "HR")
    @DisplayName("POST /api/v1/applications - 201 Created and links candidate to job")
    void createApplication_AsHR_ShouldCreateApplication() throws Exception {
        UserEntity hr = createTestUser("hr.app@banquemisr.com", Role.ROLE_HR);

        JobEntity job = jobRepo.save(JobEntity.builder()
                .title("Mobile Developer")
                .description("Flutter / iOS / Android")
                .department("Digital Banking")
                .location("Cairo")
                .status(JobStatus.OPEN)
                .createdBy(hr)
                .build());

        CandidateEntity candidate = candidateRepo.save(CandidateEntity.builder()
                .firstName("Nour")
                .lastName("Ibrahim")
                .email("nour.ibrahim@example.com")
                .yearsOfExperience(4)
                .build());

        ApplicationRequest request = ApplicationRequest.builder()
                .jobId(job.getJobId())
                .candidateId(candidate.getCandidateId())
                .build();

        mockMvc.perform(post("/api/v1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.jobId").value(job.getJobId()))
                .andExpect(jsonPath("$.candidateId").value(candidate.getCandidateId()));

        assertTrue(applicationRepo.existsByJobJobIdAndCandidateCandidateId(job.getJobId(), candidate.getCandidateId()));
    }

    @Test
    @WithMockUser(roles = "HR")
    @DisplayName("PATCH /api/v1/applications/{id}/status - 200 OK transitions from APPLIED to SCREENING")
    void transitionStatus_ValidTransition_ShouldUpdateStatus() throws Exception {
        UserEntity hr = createTestUser("hr.transition@banquemisr.com", Role.ROLE_HR);

        JobEntity job = jobRepo.save(JobEntity.builder()
                .title("Security Engineer")
                .description("Cybersecurity")
                .department("Information Security")
                .location("Cairo")
                .status(JobStatus.OPEN)
                .createdBy(hr)
                .build());

        CandidateEntity candidate = candidateRepo.save(CandidateEntity.builder()
                .firstName("Karim")
                .lastName("Mostafa")
                .email("karim.mostafa@example.com")
                .yearsOfExperience(6)
                .build());

        ApplicationEntity application = applicationRepo.save(ApplicationEntity.builder()
                .job(job)
                .candidate(candidate)
                .build());

        TransitionStatusRequest request = TransitionStatusRequest.builder()
                .newStatus(ApplicationStatus.SCREENING)
                .build();

        mockMvc.perform(patch("/api/v1/applications/{id}/status", application.getApplicationId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCREENING"));
    }
}
