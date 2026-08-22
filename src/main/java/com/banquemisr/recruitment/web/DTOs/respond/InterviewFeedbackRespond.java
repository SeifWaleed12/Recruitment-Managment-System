package com.banquemisr.recruitment.web.DTOs.respond;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewFeedbackRespond {
    private String feedbackId;
    private String applicationId;
    private String interviewerId;
    private String interviewerName;
    private Instant interviewDate;
    private Double overallScore;
    private String comments;
}
