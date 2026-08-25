package com.banquemisr.recruitment.web.controller;

import com.banquemisr.recruitment.data.enums.Role;
import com.banquemisr.recruitment.service.UserService;
import com.banquemisr.recruitment.web.DTOs.request.UserRequest;
import com.banquemisr.recruitment.web.DTOs.respond.UserRespond;
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
 * Controller-slice tests for {@link UserController}.
 *
 * Same approach as the other controller tests in this suite: @WebMvcTest
 * with a minimal nested @TestConfiguration standing in for SecurityConfig
 * (stateless, CSRF disabled, same 401/403 semantics, @EnableMethodSecurity
 * active so @PreAuthorize is enforced).
 *
 * NOTES on the controller as written (tested faithfully, not "corrected"):
 * - createAdmin() requires hasRole('HR'), not ADMIN -- tests reflect that.
 * - changePassword() has no @PreAuthorize -- any authenticated role passes;
 *   only unauthenticated requests are rejected.
 */
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    // JwtAuthenticationFilter is a @Component (Filter), so @WebMvcTest picks
    // it up directly and needs its dependency satisfied even though the
    // filter itself won't run any auth logic in these tests (requests are
    // pre-authenticated via @WithMockUser).
    @MockBean
    private com.banquemisr.recruitment.Authentication.Security.JwtService jwtService;

    private UserRequest validUserRequest;
    private UserRespond userResponse;

    @BeforeEach
    void setUp() {
        validUserRequest = UserRequest.builder()
                .userEmail("jane.doe@example.com")
                .userPassword("SecurePass123")
                .userFname("Jane")
                .userLname("Doe")
                .role(Role.ROLE_INTERVIEWER)
                .enabled(true)
                .build();

        userResponse = UserRespond.builder()
                .userId("user-123")
                .userEmail("jane.doe@example.com")
                .userFname("Jane")
                .userLname("Doe")
                .role(Role.ROLE_INTERVIEWER)
                .roleName("ROLE_INTERVIEWER")
                .enabled(true)
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

    // ---------- GET /api/v1/users ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_asAdmin_returnsOk() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(userResponse));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk());

        verify(userService).getAllUsers();
    }

    @Test
    @WithMockUser(roles = "HR") // not ADMIN
    void getAllUsers_withDisallowedRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsers_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- GET /api/v1/users/{id} ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserById_asAdmin_returnsOk() throws Exception {
        String userId = "user-123";
        when(userService.getUserById(userId)).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/users/{id}", userId))
                .andExpect(status().isOk());

        verify(userService).getUserById(userId);
    }

    @Test
    @WithMockUser(roles = "HR")
    void getUserById_withDisallowedRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", "user-123"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserById_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", "user-123"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- POST /api/v1/users ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_asAdmin_returnsCreated() throws Exception {
        when(userService.createUser(any(UserRequest.class))).thenReturn(userResponse);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isCreated());

        verify(userService).createUser(any(UserRequest.class));
    }

    @Test
    @WithMockUser(roles = "HR") // not ADMIN
    void createUser_withDisallowedRole_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUser_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_withInvalidBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- POST /api/v1/users/interviewer ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void createInterviewer_asAdmin_returnsCreated() throws Exception {
        when(userService.createInterviewer(any(UserRequest.class))).thenReturn(userResponse);

        mockMvc.perform(post("/api/v1/users/interviewer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isCreated());

        verify(userService).createInterviewer(any(UserRequest.class));
    }

    @Test
    @WithMockUser(roles = "HR")
    void createInterviewer_asHr_returnsCreated() throws Exception {
        when(userService.createInterviewer(any(UserRequest.class))).thenReturn(userResponse);

        mockMvc.perform(post("/api/v1/users/interviewer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "INTERVIEWER") // not allowed
    void createInterviewer_withDisallowedRole_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/users/interviewer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createInterviewer_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/users/interviewer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isUnauthorized());
    }

    // ---------- POST /api/v1/users/hr ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void createHr_asAdmin_returnsCreated() throws Exception {
        when(userService.createHr(any(UserRequest.class))).thenReturn(userResponse);

        mockMvc.perform(post("/api/v1/users/hr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isCreated());

        verify(userService).createHr(any(UserRequest.class));
    }

    @Test
    @WithMockUser(roles = "HR") // not allowed -- only ADMIN per hasAnyRole('ADMIN')
    void createHr_withDisallowedRole_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/users/hr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createHr_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/users/hr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isUnauthorized());
    }

    // ---------- POST /api/v1/users/admin ----------
    // NOTE: controller requires hasRole('HR') here, not ADMIN -- tested as written.

    @Test
    @WithMockUser(roles = "HR")
    void createAdmin_asHr_returnsCreated() throws Exception {
        when(userService.createAdmin(any(UserRequest.class))).thenReturn(userResponse);

        mockMvc.perform(post("/api/v1/users/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isCreated());

        verify(userService).createAdmin(any(UserRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN") // NOT allowed per current @PreAuthorize("hasRole('HR')")
    void createAdmin_asAdmin_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/users/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createAdmin_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/users/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isUnauthorized());
    }

    // ---------- PATCH /api/v1/users/{id}/role ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void changeUserRole_asAdmin_returnsOk() throws Exception {
        String userId = "user-123";
        when(userService.changeUserRole(userId, Role.ROLE_HR)).thenReturn(userResponse);

        mockMvc.perform(patch("/api/v1/users/{id}/role", userId)
                        .param("role", "ROLE_HR"))
                .andExpect(status().isOk());

        verify(userService).changeUserRole(userId, Role.ROLE_HR);
    }

    @Test
    @WithMockUser(roles = "HR")
    void changeUserRole_withDisallowedRole_returnsForbidden() throws Exception {
        mockMvc.perform(patch("/api/v1/users/{id}/role", "user-123")
                        .param("role", "ROLE_HR"))
                .andExpect(status().isForbidden());
    }

    @Test
    void changeUserRole_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/v1/users/{id}/role", "user-123")
                        .param("role", "ROLE_HR"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- PUT /api/v1/users/{id} ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_asAdmin_returnsOk() throws Exception {
        String userId = "user-123";
        when(userService.updateUser(eq(userId), any(UserRequest.class))).thenReturn(userResponse);

        mockMvc.perform(put("/api/v1/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isOk());

        verify(userService).updateUser(eq(userId), any(UserRequest.class));
    }

    @Test
    @WithMockUser(roles = "HR")
    void updateUser_withDisallowedRole_returnsForbidden() throws Exception {
        mockMvc.perform(put("/api/v1/users/{id}", "user-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_withInvalidBody_returnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/v1/users/{id}", "user-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUser_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(put("/api/v1/users/{id}", "user-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isUnauthorized());
    }

    // ---------- PATCH /api/v1/users/{id}/password ----------
    // NOTE: no @PreAuthorize on this endpoint -- any authenticated role passes.

    @Test
    @WithMockUser(roles = "ADMIN")
    void changePassword_asAdmin_returnsNoContent() throws Exception {
        String userId = "user-123";
        doNothing().when(userService).changePassword(userId, "oldPass123", "newPass456");

        mockMvc.perform(patch("/api/v1/users/{id}/password", userId)
                        .param("oldPassword", "oldPass123")
                        .param("newPassword", "newPass456"))
                .andExpect(status().isNoContent());

        verify(userService).changePassword(userId, "oldPass123", "newPass456");
    }

    @Test
    @WithMockUser(roles = "INTERVIEWER") // any authenticated role is allowed here
    void changePassword_asAnyAuthenticatedRole_returnsNoContent() throws Exception {
        String userId = "user-123";
        doNothing().when(userService).changePassword(userId, "oldPass123", "newPass456");

        mockMvc.perform(patch("/api/v1/users/{id}/password", userId)
                        .param("oldPassword", "oldPass123")
                        .param("newPassword", "newPass456"))
                .andExpect(status().isNoContent());
    }

    @Test
    void changePassword_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/v1/users/{id}/password", "user-123")
                        .param("oldPassword", "oldPass123")
                        .param("newPassword", "newPass456"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- PATCH /api/v1/users/{id}/enabled ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void toggleUserEnabled_asAdmin_returnsOk() throws Exception {
        String userId = "user-123";
        when(userService.toggleUserEnabled(userId, false)).thenReturn(userResponse);

        mockMvc.perform(patch("/api/v1/users/{id}/enabled", userId)
                        .param("enabled", "false"))
                .andExpect(status().isOk());

        verify(userService).toggleUserEnabled(userId, false);
    }

    @Test
    @WithMockUser(roles = "HR")
    void toggleUserEnabled_withDisallowedRole_returnsForbidden() throws Exception {
        mockMvc.perform(patch("/api/v1/users/{id}/enabled", "user-123")
                        .param("enabled", "false"))
                .andExpect(status().isForbidden());
    }

    @Test
    void toggleUserEnabled_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/v1/users/{id}/enabled", "user-123")
                        .param("enabled", "false"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- DELETE /api/v1/users/{id} ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_asAdmin_returnsNoContent() throws Exception {
        String userId = "user-123";
        doNothing().when(userService).deleteUser(userId);

        mockMvc.perform(delete("/api/v1/users/{id}", userId))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(userId);
    }

    @Test
    @WithMockUser(roles = "HR")
    void deleteUser_withDisallowedRole_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{id}", "user-123"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{id}", "user-123"))
                .andExpect(status().isUnauthorized());
    }
}