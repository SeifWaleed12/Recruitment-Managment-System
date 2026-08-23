package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.data.entity.InterviewFeedbackEntity;
import com.banquemisr.recruitment.data.repo.InterviewFeedbackRepo;
import com.banquemisr.recruitment.exception.ResourceNotFoundException;
import com.banquemisr.recruitment.mapper.InterviewFeedbackMapper;
import com.banquemisr.recruitment.web.DTOs.request.InterviewFeedbackUpdateRequest;
import com.banquemisr.recruitment.web.DTOs.respond.InterviewFeedbackRespond;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewFeedbackService {
    private final InterviewFeedbackRepo interviewFeedbackRepo;
    private final InterviewFeedbackMapper interviewFeedbackMapper;

    @Transactional
    public InterviewFeedbackRespond submitFeedback(
            String feedbackId, InterviewFeedbackUpdateRequest request, String currentUserEmail) {

        InterviewFeedbackEntity feedback = interviewFeedbackRepo.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview feedback not found with ID: " + feedbackId));

        if (!feedback.getInterviewer().getUserEmail().equalsIgnoreCase(currentUserEmail)) {
            throw new AccessDeniedException("Only the assigned interviewer can submit this feedback");
        }

        feedback.setOverallScore(request.getOverallScore());
        feedback.setComments(request.getComments());

        InterviewFeedbackEntity saved = interviewFeedbackRepo.save(feedback);
        return interviewFeedbackMapper.toRespond(saved);
    }

    @Transactional(readOnly = true)
    public List<InterviewFeedbackRespond> getFeedbackForApplication(String applicationId) {
        return interviewFeedbackRepo.findByApplicationApplicationId(applicationId).stream()
                .map(interviewFeedbackMapper::toRespond)
                .collect(Collectors.toList());
    }
}
