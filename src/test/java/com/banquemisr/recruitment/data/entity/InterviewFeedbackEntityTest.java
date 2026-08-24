package com.banquemisr.recruitment.data.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InterviewFeedbackEntityTest {

    @Test
    void builder_whenScheduled_hasNullScoreAndComments() {
        InterviewFeedbackEntity feedback = InterviewFeedbackEntity.builder()
                .interviewDate(Instant.parse("2026-09-01T13:00:00Z"))
                .build();

        assertThat(feedback.getOverallScore()).isNull();
        assertThat(feedback.getComments()).isNull();
    }

    @Test
    void settingScoreAndComments_afterInterview_updatesFields() {
        InterviewFeedbackEntity feedback = InterviewFeedbackEntity.builder()
                .interviewDate(Instant.parse("2026-09-01T13:00:00Z"))
                .build();

        feedback.setOverallScore(7.5);
        feedback.setComments("Strong technical answers, good communication.");

        assertThat(feedback.getOverallScore()).isEqualTo(7.5);
        assertThat(feedback.getComments()).isEqualTo("Strong technical answers, good communication.");
    }

    @Test
    void builder_withApplicationAndInterviewer_linksCorrectly() {
        ApplicationEntity application = ApplicationEntity.builder().build();
        UserEntity interviewer = UserEntity.builder().userFname("Ahmed").userLname("Ali").build();

        InterviewFeedbackEntity feedback = InterviewFeedbackEntity.builder()
                .application(application)
                .interviewer(interviewer)
                .interviewDate(Instant.now())
                .build();

        assertThat(feedback.getApplication()).isSameAs(application);
        assertThat(feedback.getInterviewer()).isSameAs(interviewer);
    }

    @Test
    void interviewDate_isStoredExactlyAsProvided() {
        Instant scheduledTime = Instant.parse("2026-09-01T13:00:00Z");

        InterviewFeedbackEntity feedback = InterviewFeedbackEntity.builder()
                .interviewDate(scheduledTime)
                .build();

        assertThat(feedback.getInterviewDate()).isEqualTo(scheduledTime);
    }
}