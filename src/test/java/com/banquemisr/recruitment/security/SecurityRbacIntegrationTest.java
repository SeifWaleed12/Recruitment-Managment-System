package com.banquemisr.recruitment.security;

import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.enums.Role;
import com.banquemisr.recruitment.data.repo.CandidateRepo;
import com.banquemisr.recruitment.data.repo.UserRepo;
import com.banquemisr.recruitment.web.DTOs.request.CandidateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityRbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private CandidateRepo candidateRepo;

    @BeforeEach
    void setUp() {
        candidateRepo.deleteAll();
        userRepo.deleteAll();

        userRepo.save(UserEntity.builder()
                .userEmail("admin@banquemisr.com")
                .userPassword("hashed_pass")
                .userFname("Admin")
                .userLname("User")
                .role(Role.ROLE_ADMIN)
                .enabled(true)
                .build());
    }

    @Test
    @DisplayName("Unauthenticated request to protected endpoint should return 401 Unauthorized")
    void testUnauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/candidates"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "interviewer@banquemisr.com", roles = {"INTERVIEWER"})
    @DisplayName("INTERVIEWER role can read candidates and jobs (200 OK)")
    void testInterviewerCanReadCandidatesAndJobs() throws Exception {
        mockMvc.perform(get("/api/v1/candidates"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "interviewer@banquemisr.com", roles = {"INTERVIEWER"})
    @DisplayName("INTERVIEWER cannot mutate candidates (403 Forbidden)")
    void testInterviewerCannotCreateOrDeleteCandidate() throws Exception {
        CandidateRequest request = CandidateRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("+201000000000")
                .yearsOfExperience(3)
                .build();

        mockMvc.perform(post("/api/v1/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        mockMvc.perform(delete("/api/v1/candidates/dummy-id"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "interviewer@banquemisr.com", roles = {"INTERVIEWER"})
    @DisplayName("INTERVIEWER cannot access Admin-only User management (403 Forbidden)")
    void testInterviewerCannotAccessUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "hr@banquemisr.com", roles = {"HR"})
    @DisplayName("HR can create candidates (201 Created) but cannot access Users (403 Forbidden)")
    void testHrCanCreateCandidateButCannotAccessUsers() throws Exception {
        CandidateRequest request = CandidateRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .phone("+201011111111")
                .yearsOfExperience(4)
                .build();

        mockMvc.perform(post("/api/v1/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("jane.smith@example.com"));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@banquemisr.com", roles = {"ADMIN"})
    @DisplayName("ADMIN has full access to Users and Candidate creation (200 / 201)")
    void testAdminHasFullAccess() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk());

        CandidateRequest request = CandidateRequest.builder()
                .firstName("Admin")
                .lastName("Candidate")
                .email("admin.candidate@example.com")
                .phone("+201022222222")
                .yearsOfExperience(7)
                .build();

        mockMvc.perform(post("/api/v1/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
