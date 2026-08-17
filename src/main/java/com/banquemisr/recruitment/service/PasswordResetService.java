package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.repo.UserRepo;
import com.banquemisr.recruitment.exception.ResourceNotFoundException;
import com.banquemisr.recruitment.web.DTOs.request.ForgotPasswordRequest;
import com.banquemisr.recruitment.web.DTOs.request.ResetPasswordRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepo userRepo;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public void forgotPassword(ForgotPasswordRequest request) {
        // Safe check: does not reveal if email exists or not (User Enumeration protection)
        userRepo.findByUserEmail(request.getEmail()).ifPresent(user -> {
            String resetToken = "reset-" + System.currentTimeMillis() + "-" + user.getUserId();
            String resetLink = "http://localhost:8080/auth/reset-password?token=" + resetToken;
            emailService.sendPasswordResetEmail(user.getUserEmail(), resetLink);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getToken().startsWith("reset-")) {
            throw new IllegalArgumentException("Invalid or expired password reset token");
        }

        String[] parts = request.getToken().split("-");
        String userId = parts[parts.length - 1];

        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setUserPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepo.save(user);
    }
}
