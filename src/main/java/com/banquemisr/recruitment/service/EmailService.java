package com.banquemisr.recruitment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("no-reply@banquemisr.com");
        message.setTo(toEmail);
        message.setSubject("Banque Misr Recruitment Platform - Password Reset Request");
        message.setText("Dear User,\n\n"
                + "You requested to reset your password. Please click the link below to set a new password:\n\n"
                + resetLink + "\n\n"
                + "This link will expire in 15 minutes.\n\n"
                + "If you did not request this, please ignore this email.\n\n"
                + "Regards,\nBanque Misr Recruitment Team");

        mailSender.send(message);
    }
}
