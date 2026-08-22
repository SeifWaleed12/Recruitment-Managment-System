package com.banquemisr.recruitment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private static final DateTimeFormatter INTERVIEW_DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a").withZone(ZoneId.of("Africa/Cairo"));


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

    public void sendInterviewInvitation(String toEmail, String candidateName, Instant interviewDateTime) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("no-reply@banquemisr.com");
        message.setTo(toEmail);
        message.setSubject("Banque Misr Recruitment Platform - Interview Invitation");
        message.setText("Dear " + candidateName + ",\n\n"
                + "We are pleased to invite you to an interview for your application.\n\n"
                + "Date & Time: " + INTERVIEW_DATE_FORMAT.format(interviewDateTime) + "\n\n"
                + "Please make sure to be available at the scheduled time.\n\n"
                + "Regards,\nBanque Misr Recruitment Team");

        mailSender.send(message);
    }
}
