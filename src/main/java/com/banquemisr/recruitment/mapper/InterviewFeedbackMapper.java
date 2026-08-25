package com.banquemisr.recruitment.mapper;

import com.banquemisr.recruitment.data.entity.InterviewFeedbackEntity;
import com.banquemisr.recruitment.web.DTOs.respond.InterviewFeedbackRespond;
import org.springframework.stereotype.Component;

@Component
public class InterviewFeedbackMapper {
    public InterviewFeedbackRespond toRespond(InterviewFeedbackEntity entity) {
        if (entity == null) return null;
        return InterviewFeedbackRespond.builder()
                .feedbackId(entity.getFeedbackId())
                .applicationId(entity.getApplication().getApplicationId())
                .interviewerId(entity.getInterviewer().getUserId())
                .interviewerName(entity.getInterviewer().getUserFname() + " " + entity.getInterviewer().getUserLname())
                .interviewDate(entity.getInterviewDate())
                .overallScore(entity.getOverallScore())
                .comments(entity.getComments())
                .build();
    }
}
