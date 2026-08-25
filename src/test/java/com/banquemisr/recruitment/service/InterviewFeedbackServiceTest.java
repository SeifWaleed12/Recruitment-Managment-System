package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.data.entity.ApplicationEntity;
import com.banquemisr.recruitment.data.entity.InterviewFeedbackEntity;
import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.repo.InterviewFeedbackRepo;
import com.banquemisr.recruitment.exception.ResourceNotFoundException;
import com.banquemisr.recruitment.mapper.InterviewFeedbackMapper;
import com.banquemisr.recruitment.web.DTOs.request.InterviewFeedbackUpdateRequest;
import com.banquemisr.recruitment.web.DTOs.respond.InterviewFeedbackRespond;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewFeedbackServiceTest {

    @Mock
    private InterviewFeedbackRepo interviewFeedbackRepo;

    @Mock
    private InterviewFeedbackMapper interviewFeedbackMapper;

    @InjectMocks
    private InterviewFeedbackService interviewFeedbackService;

    @Test
    void submitFeedback_whenInterviewerMatches_updatesAndReturnsRespond() {

        UserEntity interviewer = UserEntity.builder()
                .userEmail("interviewer@test.com")
                .build();

        InterviewFeedbackEntity feedback = InterviewFeedbackEntity.builder()
                .feedbackId("feedback-1")
                .interviewer(interviewer)
                .build();

        InterviewFeedbackUpdateRequest request =
                InterviewFeedbackUpdateRequest.builder()
                        .overallScore(9.0)
                        .comments("Excellent candidate")
                        .build();

        InterviewFeedbackRespond respond =
                InterviewFeedbackRespond.builder()
                        .feedbackId("feedback-1")
                        .build();

        when(interviewFeedbackRepo.findById("feedback-1"))
                .thenReturn(Optional.of(feedback));

        when(interviewFeedbackRepo.save(feedback))
                .thenReturn(feedback);

        when(interviewFeedbackMapper.toRespond(feedback))
                .thenReturn(respond);

        InterviewFeedbackRespond result =
                interviewFeedbackService.submitFeedback(
                        "feedback-1",
                        request,
                        "interviewer@test.com"
                );

        assertThat(feedback.getOverallScore()).isEqualTo(9);
        assertThat(feedback.getComments()).isEqualTo("Excellent candidate");
        assertThat(result.getFeedbackId()).isEqualTo("feedback-1");

        verify(interviewFeedbackRepo).save(feedback);
    }

    @Test
    void submitFeedback_whenFeedbackNotFound_throwsResourceNotFoundException() {

        InterviewFeedbackUpdateRequest request =
                InterviewFeedbackUpdateRequest.builder().build();

        when(interviewFeedbackRepo.findById("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                interviewFeedbackService.submitFeedback(
                        "missing",
                        request,
                        "interviewer@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(interviewFeedbackRepo, never()).save(any());
    }

    @Test
    void submitFeedback_whenWrongInterviewer_throwsAccessDeniedException() {

        UserEntity interviewer = UserEntity.builder()
                .userEmail("correct@test.com")
                .build();

        InterviewFeedbackEntity feedback =
                InterviewFeedbackEntity.builder()
                        .feedbackId("feedback-1")
                        .interviewer(interviewer)
                        .build();

        InterviewFeedbackUpdateRequest request =
                InterviewFeedbackUpdateRequest.builder()
                        .overallScore(8.0)
                        .comments("Good")
                        .build();

        when(interviewFeedbackRepo.findById("feedback-1"))
                .thenReturn(Optional.of(feedback));

        assertThatThrownBy(() ->
                interviewFeedbackService.submitFeedback(
                        "feedback-1",
                        request,
                        "wrong@test.com"))
                .isInstanceOf(AccessDeniedException.class);

        verify(interviewFeedbackRepo, never()).save(any());
    }

    @Test
    void getFeedbackForApplication_returnsFeedbackList() {

        InterviewFeedbackEntity feedback =
                InterviewFeedbackEntity.builder()
                        .feedbackId("feedback-1")
                        .application(ApplicationEntity.builder()
                                .applicationId("app-1")
                                .build())
                        .build();

        InterviewFeedbackRespond respond =
                InterviewFeedbackRespond.builder()
                        .feedbackId("feedback-1")
                        .build();

        when(interviewFeedbackRepo.findByApplicationApplicationId("app-1"))
                .thenReturn(List.of(feedback));

        when(interviewFeedbackMapper.toRespond(feedback))
                .thenReturn(respond);

        List<InterviewFeedbackRespond> result =
                interviewFeedbackService.getFeedbackForApplication("app-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFeedbackId())
                .isEqualTo("feedback-1");
    }

    @Test
    void getFeedbackForApplication_whenNoFeedback_returnsEmptyList() {

        when(interviewFeedbackRepo.findByApplicationApplicationId("app-1"))
                .thenReturn(List.of());

        List<InterviewFeedbackRespond> result =
                interviewFeedbackService.getFeedbackForApplication("app-1");

        assertThat(result).isEmpty();

        verify(interviewFeedbackMapper, never()).toRespond(any());
    }
}