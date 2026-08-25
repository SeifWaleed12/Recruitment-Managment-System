package com.banquemisr.recruitment.web.controller;

import com.banquemisr.recruitment.service.InterviewFeedbackService;
import com.banquemisr.recruitment.web.DTOs.request.InterviewFeedbackUpdateRequest;
import com.banquemisr.recruitment.web.DTOs.respond.InterviewFeedbackRespond;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/interview-feedbacks")
@RequiredArgsConstructor
public class InterviewFeedbackController {
    private final InterviewFeedbackService interviewFeedbackService;

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('INTERVIEWER')")
    public InterviewFeedbackRespond submitFeedback(
            @PathVariable(name = "id") String feedbackId,
            @Valid @RequestBody InterviewFeedbackUpdateRequest request,
            Authentication authentication) {
        return this.interviewFeedbackService.submitFeedback(feedbackId, request, authentication.getName());
    }

    @GetMapping("/application/{applicationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'INTERVIEWER')")
    public List<InterviewFeedbackRespond> getFeedbackForApplication(
            @PathVariable(name = "applicationId") String applicationId) {
        return this.interviewFeedbackService.getFeedbackForApplication(applicationId);
    }
}
