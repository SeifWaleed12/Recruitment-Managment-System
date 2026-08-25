package com.banquemisr.recruitment.controller;

import com.banquemisr.recruitment.base.BaseIntegrationTest;
import com.banquemisr.recruitment.data.entity.CandidateEntity;
import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.enums.Role;
import com.banquemisr.recruitment.data.repo.CandidateRepo;
import com.banquemisr.recruitment.web.DTOs.request.CandidateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CandidateControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CandidateRepo candidateRepo;

    @Test
    @WithMockUser(roles = "HR")
    @DisplayName("POST /api/v1/candidates - 201 Created and persists candidate")
    void createCandidate_AsHR_ShouldCreateCandidate() throws Exception {
        UserEntity hrUser = createTestUser("recruiter.cand@banquemisr.com", Role.ROLE_HR);

        CandidateRequest request = CandidateRequest.builder()
                .firstName("Ahmed")
                .lastName("Hassan")
                .email("ahmed.hassan@example.com")
                .phone("+201012345678")
                .yearsOfExperience(5)
                .createdByUserId(hrUser.getUserId())
                .build();

        mockMvc.perform(post("/api/v1/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Ahmed"))
                .andExpect(jsonPath("$.lastName").value("Hassan"))
                .andExpect(jsonPath("$.email").value("ahmed.hassan@example.com"))
                .andExpect(jsonPath("$.yearsOfExperience").value(5));

        assertTrue(candidateRepo.existsByEmail("ahmed.hassan@example.com"));
    }

    @Test
    @WithMockUser(roles = "INTERVIEWER")
    @DisplayName("GET /api/v1/candidates - 200 OK returns paginated response")
    void getAllCandidates_AsInterviewer_ShouldReturnPagedResponse() throws Exception {
        CandidateEntity candidate = CandidateEntity.builder()
                .firstName("Sara")
                .lastName("Ali")
                .email("sara.ali@example.com")
                .yearsOfExperience(3)
                .build();
        candidateRepo.save(candidate);

        mockMvc.perform(get("/api/v1/candidates?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    @WithMockUser(roles = "HR")
    @DisplayName("GET /api/v1/candidates/search - 200 OK filters candidates by name")
    void searchCandidates_ByName_ShouldReturnMatches() throws Exception {
        CandidateEntity c1 = CandidateEntity.builder()
                .firstName("Omar")
                .lastName("Khaled")
                .email("omar.khaled@example.com")
                .yearsOfExperience(4)
                .build();
        candidateRepo.save(c1);

        mockMvc.perform(get("/api/v1/candidates/search?name=Omar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].firstName").value("Omar"));
    }
}
