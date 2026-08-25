package com.banquemisr.recruitment.controller;

import com.banquemisr.recruitment.base.BaseIntegrationTest;
import com.banquemisr.recruitment.web.DTOs.request.ForgotPasswordRequest;
import com.banquemisr.recruitment.web.DTOs.request.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("POST /auth/login - Validation error when fields are blank")
    void login_BlankFields_ShouldReturnBadRequest() throws Exception {
        LoginRequest request = new LoginRequest("", "");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/login - Validation error when email format is invalid")
    void login_InvalidEmail_ShouldReturnBadRequest() throws Exception {
        LoginRequest request = new LoginRequest("not-an-email", "password123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/forgot-password - Invalid email format returns 400 Bad Request")
    void forgotPassword_InvalidEmail_ShouldReturnBadRequest() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("invalid-email");

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
