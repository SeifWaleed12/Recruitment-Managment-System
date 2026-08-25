package com.banquemisr.recruitment.controller;

import com.banquemisr.recruitment.base.BaseIntegrationTest;
import com.banquemisr.recruitment.data.entity.JobEntity;
import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.enums.JobStatus;
import com.banquemisr.recruitment.data.enums.Role;
import com.banquemisr.recruitment.data.repo.JobRepo;
import com.banquemisr.recruitment.web.DTOs.request.JobRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class JobControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private JobRepo jobRepo;

    @Test
    @DisplayName("GET /api/v1/jobs - 401 Unauthorized when not authenticated")
    void getAllJobs_Unauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "HR")
    @DisplayName("POST /api/v1/jobs - 201 Created and persists when authorized as HR")
    void createJob_AsHR_ShouldCreateAndPersistJob() throws Exception {
        UserEntity hrUser = createTestUser("hr.recruiter@banquemisr.com", Role.ROLE_HR);

        JobRequest request = JobRequest.builder()
                .title("Senior Backend Engineer")
                .description("Expert in Java and Spring Boot")
                .department("Information Technology")
                .location("Cairo Headquarters")
                .status(JobStatus.OPEN)
                .createdByUserId(hrUser.getUserId())
                .build();

        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Senior Backend Engineer"))
                .andExpect(jsonPath("$.department").value("Information Technology"))
                .andExpect(jsonPath("$.status").value("OPEN"));

        // Verify database persistence
        assertTrue(jobRepo.findAll().stream()
                .anyMatch(j -> "Senior Backend Engineer".equals(j.getTitle())));
    }

    @Test
    @WithMockUser(roles = "CANDIDATE")
    @DisplayName("POST /api/v1/jobs - 403 Forbidden when unauthorized role attempts creation")
    void createJob_UnauthorizedRole_ShouldReturn403() throws Exception {
        JobRequest request = JobRequest.builder()
                .title("Unauthorized Title")
                .description("Some description")
                .department("IT")
                .location("Cairo")
                .status(JobStatus.OPEN)
                .createdByUserId("dummy-user-id")
                .build();

        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    @DisplayName("GET /api/v1/jobs/{id} - 200 OK returns job details")
    void getJobById_ExistingJob_ShouldReturnJobDetails() throws Exception {
        UserEntity hrUser = createTestUser("hr.lead@banquemisr.com", Role.ROLE_HR);

        JobEntity job = JobEntity.builder()
                .title("Data Analyst")
                .description("Data analytics role")
                .department("Analytics")
                .location("Alexandria")
                .status(JobStatus.OPEN)
                .createdBy(hrUser)
                .build();
        JobEntity savedJob = jobRepo.save(job);

        mockMvc.perform(get("/api/v1/jobs/{id}", savedJob.getJobId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Data Analyst"))
                .andExpect(jsonPath("$.department").value("Analytics"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/v1/jobs/{id} - 204 No Content deletes job")
    void deleteJob_AsAdmin_ShouldDeleteJob() throws Exception {
        UserEntity admin = createTestUser("admin.user@banquemisr.com", Role.ROLE_ADMIN);

        JobEntity job = JobEntity.builder()
                .title("Obsolete Job")
                .description("To be deleted")
                .department("HR")
                .location("Cairo")
                .status(JobStatus.CLOSED)
                .createdBy(admin)
                .build();
        JobEntity savedJob = jobRepo.save(job);

        mockMvc.perform(delete("/api/v1/jobs/{id}", savedJob.getJobId()))
                .andExpect(status().isNoContent());

        assertFalse(jobRepo.findById(savedJob.getJobId()).isPresent());
    }
}
