package com.banquemisr.recruitment.web.controller;

import com.banquemisr.recruitment.service.SkillService;
import com.banquemisr.recruitment.web.DTOs.request.SkillRequest;
import com.banquemisr.recruitment.web.DTOs.respond.SkillRespond;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-slice tests for {@link SkillController}.
 *
 * Same approach as the other controller tests in this suite: @WebMvcTest
 * with a minimal nested @TestConfiguration standing in for SecurityConfig
 * (stateless, CSRF disabled, same 401/403 semantics, @EnableMethodSecurity
 * active so @PreAuthorize is enforced).
 */
@WebMvcTest(SkillController.class)
class SkillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SkillService skillService;

    // JwtAuthenticationFilter is a @Component (Filter), so @WebMvcTest picks
    // it up directly and needs its dependency satisfied even though the
    // filter itself won't run any auth logic in these tests (requests are
    // pre-authenticated via @WithMockUser).
    @MockBean
    private com.banquemisr.recruitment.Authentication.Security.JwtService jwtService;

    private SkillRequest validSkillRequest;
    private SkillRespond skillResponse;

    @BeforeEach
    void setUp() {
        validSkillRequest = SkillRequest.builder()
                .name("Java")
                .build();

        skillResponse = new SkillRespond();
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

    // ---------- GET /api/v1/skills ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllSkills_withoutQuery_returnsOk() throws Exception {
        when(skillService.getAllSkills(null)).thenReturn(List.of(skillResponse));

        mockMvc.perform(get("/api/v1/skills"))
                .andExpect(status().isOk());

        verify(skillService).getAllSkills(null);
    }

    @Test
    @WithMockUser(roles = "HR")
    void getAllSkills_withQuery_returnsOk() throws Exception {
        when(skillService.getAllSkills("java")).thenReturn(List.of(skillResponse));

        mockMvc.perform(get("/api/v1/skills").param("query", "java"))
                .andExpect(status().isOk());

        verify(skillService).getAllSkills("java");
    }

    @Test
    @WithMockUser(roles = "INTERVIEWER")
    void getAllSkills_asInterviewer_returnsOk() throws Exception {
        when(skillService.getAllSkills(null)).thenReturn(List.of(skillResponse));

        mockMvc.perform(get("/api/v1/skills"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CANDIDATE") // role not allowed by @PreAuthorize
    void getAllSkills_withDisallowedRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/skills"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllSkills_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/skills"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- GET /api/v1/skills/{id} ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void getSkillById_asAdmin_returnsOk() throws Exception {
        String skillId = "skill-123";
        when(skillService.getSkillById(skillId)).thenReturn(skillResponse);

        mockMvc.perform(get("/api/v1/skills/{id}", skillId))
                .andExpect(status().isOk());

        verify(skillService).getSkillById(skillId);
    }

    @Test
    @WithMockUser(roles = "CANDIDATE")
    void getSkillById_withDisallowedRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/skills/{id}", "skill-123"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getSkillById_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/skills/{id}", "skill-123"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- POST /api/v1/skills ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void createSkill_asAdmin_returnsCreated() throws Exception {
        when(skillService.createSkill(any(SkillRequest.class))).thenReturn(skillResponse);

        mockMvc.perform(post("/api/v1/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSkillRequest)))
                .andExpect(status().isCreated());

        verify(skillService).createSkill(any(SkillRequest.class));
    }

    @Test
    @WithMockUser(roles = "HR")
    void createSkill_asHr_returnsCreated() throws Exception {
        when(skillService.createSkill(any(SkillRequest.class))).thenReturn(skillResponse);

        mockMvc.perform(post("/api/v1/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSkillRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "INTERVIEWER") // not allowed to create
    void createSkill_withDisallowedRole_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSkillRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createSkill_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSkillRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createSkill_withInvalidBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- DELETE /api/v1/skills/{id} ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteSkill_asAdmin_returnsNoContent() throws Exception {
        String skillId = "skill-123";
        doNothing().when(skillService).deleteSkill(skillId);

        mockMvc.perform(delete("/api/v1/skills/{id}", skillId))
                .andExpect(status().isNoContent());

        verify(skillService).deleteSkill(skillId);
    }

    @Test
    @WithMockUser(roles = "HR")
    void deleteSkill_asHr_returnsNoContent() throws Exception {
        String skillId = "skill-123";
        doNothing().when(skillService).deleteSkill(skillId);

        mockMvc.perform(delete("/api/v1/skills/{id}", skillId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "INTERVIEWER")
    void deleteSkill_withDisallowedRole_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/skills/{id}", "skill-123"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteSkill_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/v1/skills/{id}", "skill-123"))
                .andExpect(status().isUnauthorized());
    }
}