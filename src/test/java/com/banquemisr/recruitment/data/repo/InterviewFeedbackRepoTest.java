package com.banquemisr.recruitment.data.repo;

import com.banquemisr.recruitment.data.entity.ApplicationEntity;
import com.banquemisr.recruitment.data.entity.CandidateEntity;
import com.banquemisr.recruitment.data.entity.InterviewFeedbackEntity;
import com.banquemisr.recruitment.data.entity.JobEntity;
import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.enums.ApplicationStatus;
import com.banquemisr.recruitment.data.enums.JobStatus;
import com.banquemisr.recruitment.data.enums.Role;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class InterviewFeedbackRepoTest {

    @Autowired
    private InterviewFeedbackRepo interviewFeedbackRepo;

    @Autowired
    private EntityManager entityManager;

    private static class Fixture {
        ApplicationEntity application;
        UserEntity interviewer;
    }

    private Fixture persistApplicationWithInterviewer() {
        UserEntity hr = UserEntity.builder()
                .userEmail("hr-" + UUID.randomUUID() + "@example.com")
                .userFname("Sara")
                .userLname("Ahmed")
                .role(Role.ROLE_HR)
                .build();
        entityManager.persist(hr);

        UserEntity interviewer = UserEntity.builder()
                .userEmail("interviewer-" + UUID.randomUUID() + "@example.com")
                .userFname("Omar")
                .userLname("Khaled")
                .role(Role.ROLE_INTERVIEWER)
                .build();
        entityManager.persist(interviewer);

        CandidateEntity candidate = CandidateEntity.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane-" + UUID.randomUUID() + "@example.com")
                .cvFilePath("/cv/jane.pdf")
                .cvOriginalFilename("jane.pdf")
                .build();
        entityManager.persist(candidate);

        JobEntity job = JobEntity.builder()
                .title("Backend Engineer")
                .description("Build backend services.")
                .department("Engineering")
                .location("Cairo")
                .status(JobStatus.OPEN)
                .createdBy(hr)
                .build();
        entityManager.persist(job);

        ApplicationEntity application = ApplicationEntity.builder()
                .candidate(candidate)
                .job(job)
                .status(ApplicationStatus.INTERVIEW)
                .build();
        entityManager.persist(application);

        entityManager.flush();

        Fixture fixture = new Fixture();
        fixture.application = application;
        fixture.interviewer = interviewer;
        return fixture;
    }

    @Test
    void findByApplicationApplicationId_returnsFeedbackForThatApplication() {
        Fixture fixture = persistApplicationWithInterviewer();

        InterviewFeedbackEntity feedback = InterviewFeedbackEntity.builder()
                .application(fixture.application)
                .interviewer(fixture.interviewer)
                .interviewDate(Instant.parse("2026-09-01T13:00:00Z"))
                .build();
        entityManager.persist(feedback);
        entityManager.flush();

        List<InterviewFeedbackEntity> results =
                interviewFeedbackRepo.findByApplicationApplicationId(fixture.application.getApplicationId());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getFeedbackId()).isEqualTo(feedback.getFeedbackId());
    }

    @Test
    void findByApplicationApplicationId_whenNoFeedbackExists_returnsEmptyList() {
        Fixture fixture = persistApplicationWithInterviewer();

        List<InterviewFeedbackEntity> results =
                interviewFeedbackRepo.findByApplicationApplicationId(fixture.application.getApplicationId());

        assertThat(results).isEmpty();
    }

    @Test
    void findByApplicationApplicationId_doesNotReturnFeedbackForOtherApplications() {
        Fixture fixture1 = persistApplicationWithInterviewer();
        Fixture fixture2 = persistApplicationWithInterviewer();

        InterviewFeedbackEntity feedback1 = InterviewFeedbackEntity.builder()
                .application(fixture1.application)
                .interviewer(fixture1.interviewer)
                .interviewDate(Instant.parse("2026-09-01T13:00:00Z"))
                .build();
        entityManager.persist(feedback1);
        entityManager.flush();

        List<InterviewFeedbackEntity> resultsForApp2 =
                interviewFeedbackRepo.findByApplicationApplicationId(fixture2.application.getApplicationId());

        assertThat(resultsForApp2).isEmpty();
    }
}