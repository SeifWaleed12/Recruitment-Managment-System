package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.Authentication.Security.LdapUserService;
import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.repo.UserRepo;
import com.banquemisr.recruitment.exception.ResourceNotFoundException;
import com.banquemisr.recruitment.web.DTOs.request.ForgotPasswordRequest;
import com.banquemisr.recruitment.web.DTOs.request.ResetPasswordRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private EmailService emailService;

    @Mock
    private LdapUserService ldapUserService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    // ---------- forgotPassword ----------

    @Test
    void forgotPassword_whenUserExists_sendsResetEmail() {

        ForgotPasswordRequest request = new ForgotPasswordRequest("jane@example.com");
        UserEntity user = UserEntity.builder()
                .userId("user-1")
                .userEmail("jane@example.com")
                .build();

        when(userRepo.findByUserEmail("jane@example.com")).thenReturn(Optional.of(user));

        passwordResetService.forgotPassword(request);

        verify(emailService).sendPasswordResetEmail(eq("jane@example.com"), contains("reset-"));
    }

    @Test
    void forgotPassword_whenUserDoesNotExist_doesNotSendEmail() {

        ForgotPasswordRequest request = new ForgotPasswordRequest("unknown@example.com");

        when(userRepo.findByUserEmail("unknown@example.com")).thenReturn(Optional.empty());

        passwordResetService.forgotPassword(request);

        verifyNoInteractions(emailService);
    }

    // ---------- resetPassword ----------

    @Test
    void resetPassword_whenTokenNull_throwsIllegalArgumentException() {

        ResetPasswordRequest request = new ResetPasswordRequest(null, "newpassword123");

        assertThatThrownBy(() -> passwordResetService.resetPassword(request))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(ldapUserService);
    }

    @Test
    void resetPassword_whenTokenDoesNotStartWithResetPrefix_throwsIllegalArgumentException() {

        ResetPasswordRequest request = new ResetPasswordRequest("bad-token-user-1", "newpassword123");

        assertThatThrownBy(() -> passwordResetService.resetPassword(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resetPassword_whenTokenMalformed_throwsIllegalArgumentException() {

        ResetPasswordRequest request = new ResetPasswordRequest("reset-123", "newpassword123");

        assertThatThrownBy(() -> passwordResetService.resetPassword(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Malformed");
    }

    @Test
    void resetPassword_whenUserNotFound_throwsResourceNotFoundException() {

        ResetPasswordRequest request = new ResetPasswordRequest("reset-1234-user-1", "newpassword123");

        when(userRepo.findById("user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(ldapUserService);
    }

    @Test
    void resetPassword_whenValid_updatesLdapPassword() {

        ResetPasswordRequest request = new ResetPasswordRequest("reset-1234-user-1", "newpassword123");
        UserEntity user = UserEntity.builder()
                .userId("user-1")
                .userEmail("jane@example.com")
                .build();

        when(userRepo.findById("user-1")).thenReturn(Optional.of(user));

        passwordResetService.resetPassword(request);

        verify(ldapUserService).updatePassword("jane@example.com", "newpassword123");
    }
}