package com.banquemisr.recruitment.controller;

import com.banquemisr.recruitment.base.BaseIntegrationTest;
import com.banquemisr.recruitment.data.entity.ApplicationEntity;
import com.banquemisr.recruitment.data.entity.CandidateEntity;
import com.banquemisr.recruitment.data.entity.InterviewFeedbackEntity;
import com.banquemisr.recruitment.data.entity.JobEntity;
import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.enums.ApplicationStatus;
import com.banquemisr.recruitment.data.enums.JobStatus;
import com.banquemisr.recruitment.data.enums.Role;
import com.banquemisr.recruitment.data.repo.ApplicationRepo;
import com.banquemisr.recruitment.data.repo.CandidateRepo;
import com.banquemisr.recruitment.data.repo.InterviewFeedbackRepo;
import com.banquemisr.recruitment.data.repo.JobRepo;
import com.banquemisr.recruitment.web.DTOs.request.InterviewFeedbackUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InterviewFeedbackControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private InterviewFeedbackRepo feedbackRepo;

    @Autowired
    private ApplicationRepo applicationRepo;

    @Autowired
    private CandidateRepo candidateRepo;

    @Autowired
    private JobRepo jobRepo;

    @Test
    @WithMockUser(username = "interviewer@banquemisr.com", roles = "INTERVIEWER")
    @DisplayName("PATCH /api/v1/interview-feedbacks/{id} - 200 OK submits feedback by assigned interviewer")
    void submitFeedback_AsAssignedInterviewer_ShouldUpdateFeedback() throws Exception {
        UserEntity hr = createTestUser("hr.feedback@banquemisr.com", Role.ROLE_HR);
        UserEntity interviewer = createTestUser("interviewer@banquemisr.com", Role.ROLE_INTERVIEWER);

        JobEntity job = jobRepo.save(JobEntity.builder()
                .title("DevOps Engineer")
                .description("CI/CD and Kubernetes")
                .department("IT")
                .location("Cairo")
                .status(JobStatus.OPEN)
                .createdBy(hr)
                .build());

        CandidateEntity candidate = candidateRepo.save(CandidateEntity.builder()
                .firstName("Hassan")
                .lastName("Tarek")
                .email("hassan.tarek@example.com")
                .build());

        ApplicationEntity application = applicationRepo.save(ApplicationEntity.builder()
                .job(job)
                .candidate(candidate)
                .build());

        InterviewFeedbackEntity feedback = feedbackRepo.save(InterviewFeedbackEntity.builder()
                .application(application)
                .interviewer(interviewer)
                .interviewDate(Instant.now())
                .build());

        InterviewFeedbackUpdateRequest updateRequest = InterviewFeedbackUpdateRequest.builder()
                .overallScore(8.5)
                .comments("Strong problem-solving skills and technical depth.")
                .build();

        mockMvc.perform(patch("/api/v1/interview-feedbacks/{id}", feedback.getFeedbackId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallScore").value(8.5))
                .andExpect(jsonPath("$.comments").value("Strong problem-solving skills and technical depth."));

        InterviewFeedbackEntity saved = feedbackRepo.findById(feedback.getFeedbackId()).orElseThrow();
        assertEquals(8.5, saved.getOverallScore());
    }

    @Test
    @WithMockUser(username = "other.interviewer@banquemisr.com", roles = "INTERVIEWER")
    @DisplayName("PATCH /api/v1/interview-feedbacks/{id} - 403 Forbidden when submitted by non-assigned interviewer")
    void submitFeedback_AsUnassignedInterviewer_ShouldReturn403() throws Exception {
        UserEntity hr = createTestUser("hr.feedback2@banquemisr.com", Role.ROLE_HR);
        UserEntity assignedInterviewer = createTestUser("assigned.int@banquemisr.com", Role.ROLE_INTERVIEWER);

        JobEntity job = jobRepo.save(JobEntity.builder()
                .title("QA Engineer")
                .description("Automation testing")
                .department("QA")
                .location("Cairo")
                .status(JobStatus.OPEN)
                .createdBy(hr)
                .build());

        CandidateEntity candidate = candidateRepo.save(CandidateEntity.builder()
                .firstName("Mona")
                .lastName("Adel")
                .email("mona.adel@example.com")
                .build());

        ApplicationEntity application = applicationRepo.save(ApplicationEntity.builder()
                .job(job)
                .candidate(candidate)
                .build());

        InterviewFeedbackEntity feedback = feedbackRepo.save(InterviewFeedbackEntity.builder()
                .application(application)
                .interviewer(assignedInterviewer)
                .interviewDate(Instant.now())
                .build());

        InterviewFeedbackUpdateRequest updateRequest = InterviewFeedbackUpdateRequest.builder()
                .overallScore(7.0)
                .comments("Good communication.")
                .build();

        mockMvc.perform(patch("/api/v1/interview-feedbacks/{id}", feedback.getFeedbackId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }
}
