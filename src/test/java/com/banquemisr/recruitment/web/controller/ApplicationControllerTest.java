package com.banquemisr.recruitment.web.controller;

import com.banquemisr.recruitment.data.enums.ApplicationStatus;
import com.banquemisr.recruitment.service.ApplicationService;
import com.banquemisr.recruitment.web.DTOs.respond.ApplicationRespond;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApplicationController.class)
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApplicationService applicationService;

    // ---------- GET /{id} — accessible to all three roles ----------

    @Test
    @WithMockUser(roles = "HR")
    void getApplicationById_asHr_returns200() throws Exception {
        ApplicationRespond respond = ApplicationRespond.builder().applicationId("app-1").build();
        when(applicationService.getApplicationById("app-1")).thenReturn(respond);

        mockMvc.perform(get("/api/v1/applications/app-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value("app-1"));
    }

    @Test
    @WithMockUser(roles = "INTERVIEWER")
    void getApplicationById_asInterviewer_returns200() throws Exception {
        ApplicationRespond respond = ApplicationRespond.builder().applicationId("app-1").build();
        when(applicationService.getApplicationById("app-1")).thenReturn(respond);

        mockMvc.perform(get("/api/v1/applications/app-1"))
                .andExpect(status().isOk());
    }

    @Test
    void getApplicationById_whenNotAuthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/applications/app-1"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- POST / — ADMIN and HR only, NOT interviewer ----------

    @Test
    @WithMockUser(roles = "HR")
    void createApplication_asHr_returns201() throws Exception {
        ApplicationRespond respond = ApplicationRespond.builder().applicationId("app-1").build();
        when(applicationService.createApplication(any())).thenReturn(respond);

        String requestBody = """
                {"candidateId": "cand-1", "jobId": "job-1"}
                """;

        mockMvc.perform(post("/api/v1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicationId").value("app-1"));
    }

    @Test
    @WithMockUser(roles = "INTERVIEWER")
    void createApplication_asInterviewer_returns403() throws Exception {
        String requestBody = """
                {"candidateId": "cand-1", "jobId": "job-1"}
                """;

        mockMvc.perform(post("/api/v1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void createApplication_withMissingCandidateId_returns400() throws Exception {
        String requestBody = """
                {"jobId": "job-1"}
                """;

        mockMvc.perform(post("/api/v1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // ---------- PATCH /{id}/status — ADMIN and HR only ----------

    @Test
    @WithMockUser(roles = "HR")
    void transitionStatus_asHr_returns200() throws Exception {
        ApplicationRespond respond = ApplicationRespond.builder().applicationId("app-1").build();
        when(applicationService.transitionStatus(anyString(), any(), any(), any())).thenReturn(respond);

        String requestBody = """
                {"newStatus": "SCREENING"}
                """;

        mockMvc.perform(patch("/api/v1/applications/app-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "INTERVIEWER")
    void transitionStatus_asInterviewer_returns403() throws Exception {
        String requestBody = """
                {"newStatus": "SCREENING"}
                """;

        mockMvc.perform(patch("/api/v1/applications/app-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void transitionStatus_withMissingNewStatus_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/applications/app-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- DELETE /{id} — ADMIN and HR only ----------

    @Test
    @WithMockUser(roles = "HR")
    void deleteApplication_asHr_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/applications/app-1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "INTERVIEWER")
    void deleteApplication_asInterviewer_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/applications/app-1"))
                .andExpect(status().isForbidden());
    }

    // ---------- GET /job/{jobId} ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void getApplicationsByJobId_returnsListAs200() throws Exception {
        when(applicationService.getApplicationsByJobId("job-1"))
                .thenReturn(List.of(ApplicationRespond.builder().applicationId("app-1").build()));

        mockMvc.perform(get("/api/v1/applications/job/job-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].applicationId").value("app-1"));
    }
}