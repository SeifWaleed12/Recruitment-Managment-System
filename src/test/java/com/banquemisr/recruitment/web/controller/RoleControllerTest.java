package com.banquemisr.recruitment.web.controller;

import com.banquemisr.recruitment.service.RoleService;
import com.banquemisr.recruitment.web.DTOs.respond.RoleRespond;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-slice tests for {@link RoleController}.
 *
 * Same approach as InterviewFeedbackControllerTest / JobControllerTest:
 * @WebMvcTest with a minimal nested @TestConfiguration standing in for
 * SecurityConfig (stateless, CSRF disabled, same 401/403 semantics,
 * @EnableMethodSecurity active). The @PreAuthorize("hasRole('ADMIN')") here
 * is class-level, so it applies to every endpoint in the controller.
 *
 * NOTE: RoleRespond is built as an empty object below (assuming a Lombok
 * @NoArgsConstructor, as with the other DTOs in this codebase). These tests
 * only assert status codes and service interactions, not response body
 * content, so the empty object is fine as-is.
 */
@WebMvcTest(RoleController.class)
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoleService roleService;

    // JwtAuthenticationFilter is a @Component (Filter), so @WebMvcTest picks
    // it up directly and needs its dependency satisfied even though the
    // filter itself won't run any auth logic in these tests (requests are
    // pre-authenticated via @WithMockUser).
    @MockBean
    private com.banquemisr.recruitment.Authentication.Security.JwtService jwtService;

    private RoleRespond roleResponse;

    @BeforeEach
    void setUp() {
        roleResponse = new RoleRespond();
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

    // ---------- GET /api/v1/roles ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllRoles_asAdmin_returnsOk() throws Exception {
        when(roleService.getAllRoles()).thenReturn(List.of(roleResponse));

        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isOk());

        verify(roleService).getAllRoles();
    }

    @Test
    @WithMockUser(roles = "HR") // not ADMIN
    void getAllRoles_withDisallowedRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "INTERVIEWER") // not ADMIN
    void getAllRoles_asInterviewer_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllRoles_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- GET /api/v1/roles/{name} ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void getRoleByName_asAdmin_returnsOk() throws Exception {
        String roleName = "INTERVIEWER";
        when(roleService.getRoleByName(roleName)).thenReturn(roleResponse);

        mockMvc.perform(get("/api/v1/roles/{name}", roleName))
                .andExpect(status().isOk());

        verify(roleService).getRoleByName(roleName);
    }

    @Test
    @WithMockUser(roles = "HR") // not ADMIN
    void getRoleByName_withDisallowedRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/roles/{name}", "INTERVIEWER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRoleByName_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/roles/{name}", "INTERVIEWER"))
                .andExpect(status().isUnauthorized());
    }
}