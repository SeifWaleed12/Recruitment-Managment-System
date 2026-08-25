package com.banquemisr.recruitment.web.controller;

import com.banquemisr.recruitment.data.enums.JobStatus;
import com.banquemisr.recruitment.service.JobService;
import com.banquemisr.recruitment.web.DTOs.request.JobRequest;
import com.banquemisr.recruitment.web.DTOs.respond.JobRespond;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-slice tests for {@link JobController}.
 *
 * Same approach as InterviewFeedbackControllerTest: @WebMvcTest with a
 * minimal nested @TestConfiguration standing in for SecurityConfig (stateless,
 * CSRF disabled, same 401/403 semantics, @EnableMethodSecurity active so
 * @PreAuthorize is actually enforced) instead of loading the real
 * SecurityConfig with its LDAP/JWT infrastructure.
 *
 * TODO: jobResponse is populated with the same values as validJobRequest for
 * simplicity — adjust if your real service responses differ (e.g. generated
 * jobId format).
 */
@WebMvcTest(JobController.class)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JobService jobService;

    // JwtAuthenticationFilter is a @Component (Filter), so @WebMvcTest picks
    // it up directly and needs its dependency satisfied even though the
    // filter itself won't run any auth logic in these tests (requests are
    // pre-authenticated via @WithMockUser).
    @MockBean
    private com.banquemisr.recruitment.Authentication.Security.JwtService jwtService;

    private JobRequest validJobRequest;
    private JobRespond jobResponse;

    @BeforeEach
    void setUp() {
        validJobRequest = JobRequest.builder()
                .title("Senior Backend Engineer")
                .description("Design and build backend services for the recruitment platform.")
                .department("Engineering")
                .location("Cairo, Egypt")
                .status(JobStatus.OPEN)
                .createdByUserId("user-001")
                .build();

        jobResponse = JobRespond.builder()
                .jobId("job-123")
                .title("Senior Backend Engineer")
                .description("Design and build backend services for the recruitment platform.")
                .department("Engineering")
                .location("Cairo, Egypt")
                .status(JobStatus.OPEN)
                .createdByUserId("user-001")
                .build();
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {

        @Bean
        public SecurityFilterChain testSecurityFilterChain(
                HttpSecurity http,
                AuthenticationEntryPoint authenticationEntryPoint,
                AccessDeniedHandler accessDeniedHandler) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(session ->
                            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .exceptionHandling(ex -> ex
                            .authenticationEntryPoint(authenticationEntryPoint)
                            .accessDeniedHandler(accessDeniedHandler))
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
            return http.build();
        }

        @Bean
        public AuthenticationEntryPoint authenticationEntryPoint() {
            return (request, response, authException) ->
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }

        @Bean
        public AccessDeniedHandler accessDeniedHandler() {
            return (request, response, accessDeniedException) ->
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    // ---------- GET /api/v1/jobs ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllJobs_withoutStatus_returnsOk() throws Exception {
        when(jobService.getAllJobs()).thenReturn(List.of(jobResponse));

        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isOk());

        verify(jobService).getAllJobs();
    }

    @Test
    @WithMockUser(roles = "HR")
    void getAllJobs_withStatus_filtersByStatus() throws Exception {
        when(jobService.getJobsByStatus(JobStatus.OPEN)).thenReturn(List.of(jobResponse));

        mockMvc.perform(get("/api/v1/jobs").param("status", "OPEN"))
                .andExpect(status().isOk());

        verify(jobService).getJobsByStatus(JobStatus.OPEN);
    }

    @Test
    @WithMockUser(roles = "INTERVIEWER")
    void getAllJobs_asInterviewer_returnsOk() throws Exception {
        when(jobService.getAllJobs()).thenReturn(List.of(jobResponse));

        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CANDIDATE") // role not allowed by @PreAuthorize
    void getAllJobs_withDisallowedRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllJobs_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllJobs_withInvalidStatus_returnsServerError() throws Exception {
        // NOTE: MethodArgumentTypeMismatchException (invalid enum conversion)
        // isn't mapped to 400 by this app's exception handling either, so it
        // also falls through to the generic 500 handler.
        mockMvc.perform(get("/api/v1/jobs").param("status", "NOT_A_REAL_STATUS"))
                .andExpect(status().isInternalServerError());
    }

    // ---------- GET /api/v1/jobs/{id} ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void getJobById_asAdmin_returnsOk() throws Exception {
        String jobId = "job-123";
        when(jobService.getJobById(jobId)).thenReturn(jobResponse);

        mockMvc.perform(get("/api/v1/jobs/{id}", jobId))
                .andExpect(status().isOk());

        verify(jobService).getJobById(jobId);
    }

    @Test
    @WithMockUser(roles = "CANDIDATE")
    void getJobById_withDisallowedRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/{id}", "job-123"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getJobById_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/{id}", "job-123"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- POST /api/v1/jobs ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void createJob_asAdmin_returnsCreated() throws Exception {
        when(jobService.createJob(any(JobRequest.class))).thenReturn(jobResponse);

        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validJobRequest)))
                .andExpect(status().isCreated());

        verify(jobService).createJob(any(JobRequest.class));
    }

    @Test
    @WithMockUser(roles = "HR")
    void createJob_asHr_returnsCreated() throws Exception {
        when(jobService.createJob(any(JobRequest.class))).thenReturn(jobResponse);

        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validJobRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "INTERVIEWER") // not allowed to create
    void createJob_withDisallowedRole_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validJobRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createJob_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validJobRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createJob_withInvalidBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- PUT /api/v1/jobs/{id} ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateJob_asAdmin_returnsOk() throws Exception {
        String jobId = "job-123";
        when(jobService.updateJob(eq(jobId), any(JobRequest.class))).thenReturn(jobResponse);

        mockMvc.perform(put("/api/v1/jobs/{id}", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validJobRequest)))
                .andExpect(status().isOk());

        verify(jobService).updateJob(eq(jobId), any(JobRequest.class));
    }

    @Test
    @WithMockUser(roles = "INTERVIEWER")
    void updateJob_withDisallowedRole_returnsForbidden() throws Exception {
        mockMvc.perform(put("/api/v1/jobs/{id}", "job-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validJobRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateJob_withInvalidBody_returnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/v1/jobs/{id}", "job-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateJob_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(put("/api/v1/jobs/{id}", "job-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validJobRequest)))
                .andExpect(status().isUnauthorized());
    }

    // ---------- DELETE /api/v1/jobs/{id} ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteJob_asAdmin_returnsNoContent() throws Exception {
        String jobId = "job-123";
        doNothing().when(jobService).deleteJob(jobId);

        mockMvc.perform(delete("/api/v1/jobs/{id}", jobId))
                .andExpect(status().isNoContent());

        verify(jobService).deleteJob(jobId);
    }

    @Test
    @WithMockUser(roles = "HR")
    void deleteJob_asHr_returnsNoContent() throws Exception {
        String jobId = "job-123";
        doNothing().when(jobService).deleteJob(jobId);

        mockMvc.perform(delete("/api/v1/jobs/{id}", jobId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "INTERVIEWER")
    void deleteJob_withDisallowedRole_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/jobs/{id}", "job-123"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteJob_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/v1/jobs/{id}", "job-123"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- PATCH /api/v1/jobs/{id}/status ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void changeStatus_asAdmin_returnsOk() throws Exception {
        String jobId = "job-123";
        when(jobService.updateJobStatus(jobId, JobStatus.CLOSED)).thenReturn(jobResponse);

        mockMvc.perform(patch("/api/v1/jobs/{id}/status", jobId)
                        .param("status", "CLOSED"))
                .andExpect(status().isOk());

        verify(jobService).updateJobStatus(jobId, JobStatus.CLOSED);
    }

    @Test
    @WithMockUser(roles = "HR")
    void changeStatus_asHr_returnsOk() throws Exception {
        String jobId = "job-123";
        when(jobService.updateJobStatus(jobId, JobStatus.CLOSED)).thenReturn(jobResponse);

        mockMvc.perform(patch("/api/v1/jobs/{id}/status", jobId)
                        .param("status", "CLOSED"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "INTERVIEWER")
    void changeStatus_withDisallowedRole_returnsForbidden() throws Exception {
        mockMvc.perform(patch("/api/v1/jobs/{id}/status", "job-123")
                        .param("status", "CLOSED"))
                .andExpect(status().isForbidden());
    }

    @Test
    void changeStatus_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/v1/jobs/{id}/status", "job-123")
                        .param("status", "CLOSED"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void changeStatus_missingStatusParam_returnsServerError() throws Exception {
        mockMvc.perform(patch("/api/v1/jobs/{id}/status", "job-123"))
                .andExpect(status().isInternalServerError());
    }
}