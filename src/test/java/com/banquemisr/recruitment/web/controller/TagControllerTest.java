package com.banquemisr.recruitment.web.controller;

import com.banquemisr.recruitment.service.TagService;
import com.banquemisr.recruitment.web.DTOs.request.TagRequest;
import com.banquemisr.recruitment.web.DTOs.respond.TagRespond;
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
 * Controller-slice tests for {@link TagController}.
 *
 * Same approach as the other controller tests in this suite: @WebMvcTest
 * with a minimal nested @TestConfiguration standing in for SecurityConfig
 * (stateless, CSRF disabled, same 401/403 semantics, @EnableMethodSecurity
 * active so @PreAuthorize is enforced).
 */
@WebMvcTest(TagController.class)
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TagService tagService;

    // JwtAuthenticationFilter is a @Component (Filter), so @WebMvcTest picks
    // it up directly and needs its dependency satisfied even though the
    // filter itself won't run any auth logic in these tests (requests are
    // pre-authenticated via @WithMockUser).
    @MockBean
    private com.banquemisr.recruitment.Authentication.Security.JwtService jwtService;

    private TagRequest validTagRequest;
    private TagRespond tagResponse;

    @BeforeEach
    void setUp() {
        validTagRequest = TagRequest.builder()
                .name("Remote")
                .build();

        tagResponse = TagRespond.builder()
                .id("tag-123")
                .name("Remote")
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

    // ---------- GET /api/v1/tags ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllTags_asAdmin_returnsOk() throws Exception {
        when(tagService.getAllTags()).thenReturn(List.of(tagResponse));

        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk());

        verify(tagService).getAllTags();
    }

    @Test
    @WithMockUser(roles = "HR")
    void getAllTags_asHr_returnsOk() throws Exception {
        when(tagService.getAllTags()).thenReturn(List.of(tagResponse));

        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "INTERVIEWER")
    void getAllTags_asInterviewer_returnsOk() throws Exception {
        when(tagService.getAllTags()).thenReturn(List.of(tagResponse));

        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CANDIDATE") // role not allowed by @PreAuthorize
    void getAllTags_withDisallowedRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllTags_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- POST /api/v1/tags ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void createTag_asAdmin_returnsCreated() throws Exception {
        when(tagService.createTag(any(TagRequest.class))).thenReturn(tagResponse);

        mockMvc.perform(post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTagRequest)))
                .andExpect(status().isCreated());

        verify(tagService).createTag(any(TagRequest.class));
    }

    @Test
    @WithMockUser(roles = "HR")
    void createTag_asHr_returnsCreated() throws Exception {
        when(tagService.createTag(any(TagRequest.class))).thenReturn(tagResponse);

        mockMvc.perform(post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTagRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "INTERVIEWER") // not allowed to create
    void createTag_withDisallowedRole_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTagRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTag_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTagRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createTag_withInvalidBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- DELETE /api/v1/tags/{id} ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteTag_asAdmin_returnsNoContent() throws Exception {
        String tagId = "tag-123";
        doNothing().when(tagService).deleteTag(tagId);

        mockMvc.perform(delete("/api/v1/tags/{id}", tagId))
                .andExpect(status().isNoContent());

        verify(tagService).deleteTag(tagId);
    }

    @Test
    @WithMockUser(roles = "HR")
    void deleteTag_asHr_returnsNoContent() throws Exception {
        String tagId = "tag-123";
        doNothing().when(tagService).deleteTag(tagId);

        mockMvc.perform(delete("/api/v1/tags/{id}", tagId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "INTERVIEWER")
    void deleteTag_withDisallowedRole_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/tags/{id}", "tag-123"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteTag_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/v1/tags/{id}", "tag-123"))
                .andExpect(status().isUnauthorized());
    }
}