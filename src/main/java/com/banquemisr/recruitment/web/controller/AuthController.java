package com.banquemisr.recruitment.web.controller;

import com.banquemisr.recruitment.service.LoginService;
import com.banquemisr.recruitment.service.PasswordResetService;
import com.banquemisr.recruitment.service.SignUpService;
import com.banquemisr.recruitment.web.DTOs.request.ForgotPasswordRequest;
import com.banquemisr.recruitment.web.DTOs.request.LoginRequest;
import com.banquemisr.recruitment.web.DTOs.request.ResetPasswordRequest;
import com.banquemisr.recruitment.web.DTOs.request.SignUpRequest;
import com.banquemisr.recruitment.web.DTOs.respond.AuthResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SignUpService signUpService;
    private final LoginService loginService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signup(@Valid @RequestBody SignUpRequest request) {
        return this.signUpService.signup(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return this.loginService.login(request);
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.OK)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        this.passwordResetService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.OK)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        this.passwordResetService.resetPassword(request);
    }
}
