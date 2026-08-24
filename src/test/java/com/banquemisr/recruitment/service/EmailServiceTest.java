package com.banquemisr.recruitment.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;


    @Test
    void sendPasswordResetEmail_sendsCorrectEmail() {

        String email = "john@test.com";
        String resetLink = "https://example.com/reset?token=123";

        emailService.sendPasswordResetEmail(email, resetLink);

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();

        assertThat(message.getFrom()).isEqualTo("no-reply@banquemisr.com");
        assertThat(message.getTo()).containsExactly(email);
        assertThat(message.getSubject())
                .isEqualTo("Banque Misr Recruitment Platform - Password Reset Request");

        assertThat(message.getText()).contains(resetLink);
        assertThat(message.getText()).contains("This link will expire in 15 minutes.");
    }

    @Test
    void sendInterviewInvitation_sendsCorrectEmail() {

        Instant interviewDate =
                Instant.parse("2026-09-01T13:00:00Z");

        emailService.sendInterviewInvitation(
                "john@test.com",
                "John",
                interviewDate
        );

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();

        assertThat(message.getFrom()).isEqualTo("no-reply@banquemisr.com");
        assertThat(message.getTo()).containsExactly("john@test.com");

        assertThat(message.getSubject())
                .isEqualTo("Banque Misr Recruitment Platform - Interview Invitation");

        assertThat(message.getText()).contains("Dear John");
        assertThat(message.getText()).contains("We are pleased to invite you");
        assertThat(message.getText()).contains("Regards");
    }
}