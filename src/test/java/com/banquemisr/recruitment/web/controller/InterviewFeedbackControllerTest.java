package com.banquemisr.recruitment.web.controller;

import com.banquemisr.recruitment.service.InterviewFeedbackService;
import com.banquemisr.recruitment.web.DTOs.request.InterviewFeedbackUpdateRequest;
import com.banquemisr.recruitment.web.DTOs.respond.InterviewFeedbackRespond;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-slice tests for {@link InterviewFeedbackController}.
 *
 * @WebMvcTest does NOT load the real SecurityConfig (it's a plain
 * @Configuration class outside the test-slice allow-list, and pulling it in
 * directly would drag in LDAP/JWT infrastructure this test doesn't need).
 * Instead, MethodSecurityTestConfig below reproduces just the security
 * behavior this controller relies on: stateless, CSRF disabled (matches
 * SecurityConfig), the same 401/403 entry point + access-denied semantics,
 * and @EnableMethodSecurity so @PreAuthorize is actually enforced.
 *
 * NOTE: adjust the field names on InterviewFeedbackUpdateRequest /
 * InterviewFeedbackRespond below to match your real DTOs.
 */
@WebMvcTest(InterviewFeedbackController.class)
class InterviewFeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InterviewFeedbackService interviewFeedbackService;

    // JwtAuthenticationFilter is a @Component (Filter), so @WebMvcTest picks
    // it up directly and needs its dependency satisfied even though the
    // filter itself won't actually run any auth logic in these tests
    // (requests are pre-authenticated via @WithMockUser).
    @MockBean
    private com.banquemisr.recruitment.Authentication.Security.JwtService jwtService;

    private InterviewFeedbackUpdateRequest updateRequest;
    private InterviewFeedbackRespond feedbackResponse;

    @BeforeEach
    void setUp() {
        updateRequest = InterviewFeedbackUpdateRequest.builder()
                .overallScore(8.5)
                .comments("Strong candidate, good communication skills.")
                .build();

        feedbackResponse = new InterviewFeedbackRespond();
        // TODO: set actual fields, e.g.:
        // feedbackResponse.setId("feedback-123");
        // feedbackResponse.setRating(4);
    }

    /**
     * Minimal stand-in for SecurityConfig, scoped to what this controller
     * test needs. Auto-detected by @WebMvcTest since it's a static nested
     * @TestConfiguration class.
     */
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

    // ---------- PATCH /api/v1/interview-feedbacks/{id} ----------

    @Test
    @WithMockUser(roles = "INTERVIEWER")
    void submitFeedback_asInterviewer_returnsOk() throws Exception {
        String feedbackId = "feedback-123";

        when(interviewFeedbackService.submitFeedback(eq(feedbackId), any(InterviewFeedbackUpdateRequest.class), anyString()))
                .thenReturn(feedbackResponse);

        mockMvc.perform(patch("/api/v1/interview-feedbacks/{id}", feedbackId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        verify(interviewFeedbackService)
                .submitFeedback(eq(feedbackId), any(InterviewFeedbackUpdateRequest.class), anyString());
    }

    @Test
    @WithMockUser(roles = "HR") // wrong role
    void submitFeedback_withoutInterviewerRole_returnsForbidden() throws Exception {
        mockMvc.perform(patch("/api/v1/interview-feedbacks/{id}", "feedback-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void submitFeedback_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/v1/interview-feedbacks/{id}", "feedback-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "INTERVIEWER")
    void submitFeedback_withInvalidBody_returnsBadRequest() throws Exception {
        // Empty body should fail @Valid if request has required fields
        mockMvc.perform(patch("/api/v1/interview-feedbacks/{id}", "feedback-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- GET /api/v1/interview-feedbacks/application/{applicationId} ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void getFeedbackForApplication_asAdmin_returnsOk() throws Exception {
        String applicationId = "app-456";
        when(interviewFeedbackService.getFeedbackForApplication(applicationId))
                .thenReturn(List.of(feedbackResponse));

        mockMvc.perform(get("/api/v1/interview-feedbacks/application/{applicationId}", applicationId))
                .andExpect(status().isOk());

        verify(interviewFeedbackService).getFeedbackForApplication(applicationId);
    }

    @Test
    @WithMockUser(roles = "HR")
    void getFeedbackForApplication_asHr_returnsOk() throws Exception {
        String applicationId = "app-456";
        when(interviewFeedbackService.getFeedbackForApplication(applicationId))
                .thenReturn(List.of(feedbackResponse));

        mockMvc.perform(get("/api/v1/interview-feedbacks/application/{applicationId}", applicationId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "INTERVIEWER")
    void getFeedbackForApplication_asInterviewer_returnsOk() throws Exception {
        String applicationId = "app-456";
        when(interviewFeedbackService.getFeedbackForApplication(applicationId))
                .thenReturn(List.of(feedbackResponse));

        mockMvc.perform(get("/api/v1/interview-feedbacks/application/{applicationId}", applicationId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CANDIDATE") // role not allowed by @PreAuthorize
    void getFeedbackForApplication_withDisallowedRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/interview-feedbacks/application/{applicationId}", "app-456"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getFeedbackForApplication_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/interview-feedbacks/application/{applicationId}", "app-456"))
                .andExpect(status().isUnauthorized());
    }
}