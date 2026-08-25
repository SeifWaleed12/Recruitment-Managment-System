package com.banquemisr.recruitment.web.controller;

import com.banquemisr.recruitment.service.LoginService;
import com.banquemisr.recruitment.service.PasswordResetService;
import com.banquemisr.recruitment.service.RefreshTokenService;
import com.banquemisr.recruitment.web.DTOs.request.ForgotPasswordRequest;
import com.banquemisr.recruitment.web.DTOs.request.LoginRequest;
import com.banquemisr.recruitment.web.DTOs.request.ResetPasswordRequest;
import com.banquemisr.recruitment.web.DTOs.respond.AuthResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-slice tests for {@link AuthController}.
 *
 * Unlike the other controllers in this suite, /auth/** is in SecurityConfig's
 * permitAll() list -- these endpoints have no @PreAuthorize and require no
 * authentication. The nested test config below mirrors that (permitAll for
 * /auth/**, authenticated elsewhere) instead of requiring authentication for
 * everything like the other controller tests do.
 */
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LoginService loginService;

    @MockBean
    private PasswordResetService passwordResetService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    // JwtAuthenticationFilter is a @Component (Filter), so @WebMvcTest picks
    // it up directly and needs its dependency satisfied even though the
    // filter itself won't run any auth logic in these tests.
    @MockBean
    private com.banquemisr.recruitment.Authentication.Security.JwtService jwtService;

    private LoginRequest validLoginRequest;
    private ForgotPasswordRequest validForgotPasswordRequest;
    private ResetPasswordRequest validResetPasswordRequest;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        validLoginRequest = LoginRequest.builder()
                .email("jane.doe@example.com")
                .password("SecurePass123")
                .build();

        validForgotPasswordRequest = new ForgotPasswordRequest("jane.doe@example.com");

        validResetPasswordRequest = new ResetPasswordRequest("reset-token-abc", "NewPass456");

        authResponse = AuthResponse.builder()
                .accessToken("access-token-abc")
                .refreshToken("refresh-token-xyz")
                .expiresInMs(3600000L)
                .build();
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class AuthTestSecurityConfig {

        @Bean
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(session ->
                            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/auth/**").permitAll()
                            .anyRequest().authenticated());
            return http.build();
        }
    }

    // ---------- POST /auth/login ----------

    @Test
    void login_withValidCredentials_returnsOk() throws Exception {
        when(loginService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk());

        verify(loginService).login(any(LoginRequest.class));
    }

    @Test
    void login_withInvalidBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- POST /auth/refresh ----------

    @Test
    void refresh_withValidToken_returnsOk() throws Exception {
        when(refreshTokenService.refreshAccessToken("refresh-token-xyz")).thenReturn(authResponse);

        mockMvc.perform(post("/auth/refresh").param("refreshToken", "refresh-token-xyz"))
                .andExpect(status().isOk());

        verify(refreshTokenService).refreshAccessToken("refresh-token-xyz");
    }

    @Test
    void refresh_withoutTokenParam_returnsServerError() throws Exception {
        // NOTE: same as JobController -- MissingServletRequestParameterException
        // isn't mapped to 400 by this app's exception handling, so it falls
        // through to the generic 500 handler.
        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isInternalServerError());
    }

    // ---------- POST /auth/forgot-password ----------

    @Test
    void forgotPassword_withValidBody_returnsOk() throws Exception {
        doNothing().when(passwordResetService).forgotPassword(any(ForgotPasswordRequest.class));

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validForgotPasswordRequest)))
                .andExpect(status().isOk());

        verify(passwordResetService).forgotPassword(any(ForgotPasswordRequest.class));
    }

    @Test
    void forgotPassword_withInvalidBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- POST /auth/reset-password ----------

    @Test
    void resetPassword_withValidBody_returnsOk() throws Exception {
        doNothing().when(passwordResetService).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validResetPasswordRequest)))
                .andExpect(status().isOk());

        verify(passwordResetService).resetPassword(any(ResetPasswordRequest.class));
    }

    @Test
    void resetPassword_withInvalidBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}