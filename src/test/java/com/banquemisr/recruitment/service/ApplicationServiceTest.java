package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.data.entity.*;
import com.banquemisr.recruitment.data.enums.ApplicationStatus;
import com.banquemisr.recruitment.data.repo.ApplicationRepo;
import com.banquemisr.recruitment.data.repo.InterviewFeedbackRepo;
import com.banquemisr.recruitment.exception.DuplicateResourceException;
import com.banquemisr.recruitment.exception.IllegalStateTransitionException;
import com.banquemisr.recruitment.exception.ResourceNotFoundException;
import com.banquemisr.recruitment.mapper.ApplicationMapper;
import com.banquemisr.recruitment.web.DTOs.request.ApplicationRequest;
import com.banquemisr.recruitment.web.DTOs.respond.ApplicationRespond;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepo applicationRepo;
    @Mock
    private CandidateService candidateService;
    @Mock
    private JobService jobService;
    @Mock
    private UserService userService;
    @Mock
    private ApplicationMapper applicationMapper;
    @Mock
    private InterviewFeedbackRepo interviewFeedbackRepo;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private ApplicationService applicationService;

    // ---------- createApplication ----------

    @Test
    void createApplication_whenNoDuplicate_savesAndReturnsRespond() {
        ApplicationRequest request = ApplicationRequest.builder()
                .candidateId("cand-1")
                .jobId("job-1")
                .build();

        CandidateEntity candidate = CandidateEntity.builder().candidateId("cand-1").build();
        JobEntity job = JobEntity.builder().jobId("job-1").build();
        ApplicationEntity savedEntity = ApplicationEntity.builder().applicationId("app-1").build();
        ApplicationRespond respond = ApplicationRespond.builder().applicationId("app-1").build();

        when(candidateService.getCandidateEntityById("cand-1")).thenReturn(candidate);
        when(jobService.getJobEntityById("job-1")).thenReturn(job);
        when(applicationRepo.existsByJobJobIdAndCandidateCandidateId("job-1", "cand-1")).thenReturn(false);
        when(applicationMapper.toEntity(request, candidate, job, null)).thenReturn(savedEntity);
        when(applicationRepo.save(savedEntity)).thenReturn(savedEntity);
        when(applicationMapper.toRespond(savedEntity)).thenReturn(respond);

        ApplicationRespond result = applicationService.createApplication(request);

        assertThat(result.getApplicationId()).isEqualTo("app-1");
        verify(applicationRepo).save(savedEntity);
    }

    @Test
    void createApplication_whenDuplicateExists_throwsDuplicateResourceException() {
        ApplicationRequest request = ApplicationRequest.builder()
                .candidateId("cand-1")
                .jobId("job-1")
                .build();

        when(candidateService.getCandidateEntityById("cand-1"))
                .thenReturn(CandidateEntity.builder().candidateId("cand-1").build());
        when(jobService.getJobEntityById("job-1"))
                .thenReturn(JobEntity.builder().jobId("job-1").build());
        when(applicationRepo.existsByJobJobIdAndCandidateCandidateId("job-1", "cand-1")).thenReturn(true);

        assertThatThrownBy(() -> applicationService.createApplication(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(applicationRepo, never()).save(any());
    }

    // ---------- getApplicationById ----------

    @Test
    void getApplicationById_whenExists_returnsRespond() {
        ApplicationEntity entity = ApplicationEntity.builder().applicationId("app-1").build();
        ApplicationRespond respond = ApplicationRespond.builder().applicationId("app-1").build();

        when(applicationRepo.findById("app-1")).thenReturn(Optional.of(entity));
        when(applicationMapper.toRespond(entity)).thenReturn(respond);

        ApplicationRespond result = applicationService.getApplicationById("app-1");

        assertThat(result.getApplicationId()).isEqualTo("app-1");
    }

    @Test
    void getApplicationById_whenNotFound_throwsResourceNotFoundException() {
        when(applicationRepo.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getApplicationById("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- transitionStatus: legal transition, no INTERVIEW side effects ----------

    @Test
    void transitionStatus_legalTransition_savesAndReturnsRespond() {
        ApplicationEntity entity = ApplicationEntity.builder()
                .applicationId("app-1")
                .status(ApplicationStatus.APPLIED)
                .build();
        ApplicationRespond respond = ApplicationRespond.builder().applicationId("app-1").build();

        when(applicationRepo.findById("app-1")).thenReturn(Optional.of(entity));
        when(applicationRepo.save(entity)).thenReturn(entity);
        when(applicationMapper.toRespond(entity)).thenReturn(respond);

        ApplicationRespond result = applicationService.transitionStatus(
                "app-1", ApplicationStatus.SCREENING, null, null);

        assertThat(entity.getStatus()).isEqualTo(ApplicationStatus.SCREENING);
        assertThat(result.getApplicationId()).isEqualTo("app-1");
        verifyNoInteractions(interviewFeedbackRepo, emailService); // no side effects for a non-INTERVIEW transition
    }

    // ---------- transitionStatus: illegal transition ----------

    @Test
    void transitionStatus_illegalTransition_throwsAndNeverSaves() {
        ApplicationEntity entity = ApplicationEntity.builder()
                .applicationId("app-1")
                .status(ApplicationStatus.APPLIED)
                .build();

        when(applicationRepo.findById("app-1")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> applicationService.transitionStatus(
                "app-1", ApplicationStatus.HIRED, null, null))
                .isInstanceOf(IllegalStateTransitionException.class);

        verify(applicationRepo, never()).save(any());
    }

    // ---------- transitionStatus: moving to INTERVIEW ----------

    @Test
    void transitionStatus_toInterview_createsFeedbackAndSendsEmail() {
        CandidateEntity candidate = CandidateEntity.builder()
                .candidateId("cand-1")
                .firstName("Jane")
                .email("jane@example.com")
                .build();
        ApplicationEntity entity = ApplicationEntity.builder()
                .applicationId("app-1")
                .status(ApplicationStatus.SCREENING)
                .candidate(candidate)
                .build();
        UserEntity interviewer = UserEntity.builder().userId("interviewer-1").build();
        Instant interviewDate = Instant.parse("2026-09-01T13:00:00Z");

        when(applicationRepo.findById("app-1")).thenReturn(Optional.of(entity));
        when(applicationRepo.save(entity)).thenReturn(entity);
        when(userService.getUserEntityById("interviewer-1")).thenReturn(interviewer);
        when(applicationMapper.toRespond(entity)).thenReturn(ApplicationRespond.builder().applicationId("app-1").build());

        applicationService.transitionStatus("app-1", ApplicationStatus.INTERVIEW, interviewDate, "interviewer-1");

        verify(interviewFeedbackRepo).save(any(InterviewFeedbackEntity.class));
        verify(emailService).sendInterviewInvitation("jane@example.com", "Jane", interviewDate);
    }

    @Test
    void transitionStatus_toInterview_withoutDate_throwsIllegalArgumentException() {
        ApplicationEntity entity = ApplicationEntity.builder()
                .applicationId("app-1")
                .status(ApplicationStatus.SCREENING)
                .build();

        when(applicationRepo.findById("app-1")).thenReturn(Optional.of(entity));
        when(applicationRepo.save(entity)).thenReturn(entity);

        assertThatThrownBy(() -> applicationService.transitionStatus(
                "app-1", ApplicationStatus.INTERVIEW, null, "interviewer-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Interview date is required");

        verifyNoInteractions(interviewFeedbackRepo, emailService);
    }

    @Test
    void transitionStatus_toInterview_withoutInterviewer_throwsIllegalArgumentException() {
        ApplicationEntity entity = ApplicationEntity.builder()
                .applicationId("app-1")
                .status(ApplicationStatus.SCREENING)
                .build();

        when(applicationRepo.findById("app-1")).thenReturn(Optional.of(entity));
        when(applicationRepo.save(entity)).thenReturn(entity);

        assertThatThrownBy(() -> applicationService.transitionStatus(
                "app-1", ApplicationStatus.INTERVIEW, Instant.now(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("interviewer must be assigned");

        verifyNoInteractions(interviewFeedbackRepo, emailService);
    }

    // ---------- assignRecruiter ----------

    @Test
    void assignRecruiter_setsRecruiterAndSaves() {
        ApplicationEntity entity = ApplicationEntity.builder().applicationId("app-1").build();
        UserEntity recruiter = UserEntity.builder().userId("recruiter-1").build();
        ApplicationRespond respond = ApplicationRespond.builder().applicationId("app-1").build();

        when(applicationRepo.findById("app-1")).thenReturn(Optional.of(entity));
        when(userService.getUserEntityById("recruiter-1")).thenReturn(recruiter);
        when(applicationRepo.save(entity)).thenReturn(entity);
        when(applicationMapper.toRespond(entity)).thenReturn(respond);

        applicationService.assignRecruiter("app-1", "recruiter-1");

        assertThat(entity.getAssignedRecruiter()).isEqualTo(recruiter);
    }

    // ---------- deleteApplication ----------

    @Test
    void deleteApplication_whenExists_deletes() {
        when(applicationRepo.existsById("app-1")).thenReturn(true);

        applicationService.deleteApplication("app-1");

        verify(applicationRepo).deleteById("app-1");
    }

    @Test
    void deleteApplication_whenNotExists_throwsResourceNotFoundException() {
        when(applicationRepo.existsById("missing")).thenReturn(false);

        assertThatThrownBy(() -> applicationService.deleteApplication("missing"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(applicationRepo, never()).deleteById(any());
    }
}