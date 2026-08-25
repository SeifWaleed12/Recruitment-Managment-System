package com.banquemisr.recruitment.data.repo;

import com.banquemisr.recruitment.data.entity.ApplicationEntity;
import com.banquemisr.recruitment.data.entity.CandidateEntity;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ApplicationRepoTest {

    @Autowired
    private ApplicationRepo applicationRepo;

    @Autowired
    private EntityManager entityManager;

    private ApplicationEntity persistApplication(ApplicationStatus status) {
        UserEntity recruiter = UserEntity.builder()
                .userEmail("hr-" + java.util.UUID.randomUUID() + "@example.com")   // ← unique every call
                .userFname("Sara")
                .userLname("Ahmed")
                .role(Role.ROLE_HR)
                .build();
        entityManager.persist(recruiter);

        CandidateEntity candidate = CandidateEntity.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane-" + java.util.UUID.randomUUID() + "@example.com")   // ← same fix here
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
                .createdBy(recruiter)
                .build();
        entityManager.persist(job);

        ApplicationEntity application = ApplicationEntity.builder()
                .candidate(candidate)
                .job(job)
                .status(status)
                .build();
        entityManager.persist(application);

        entityManager.flush();
        return application;
    }

    @Test
    void findByJobJobId_returnsApplicationsForThatJob() {
        ApplicationEntity saved = persistApplication(ApplicationStatus.APPLIED);

        List<ApplicationEntity> results = applicationRepo.findByJobJobId(saved.getJob().getJobId());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getApplicationId()).isEqualTo(saved.getApplicationId());
    }

    @Test
    void findByCandidateCandidateId_returnsApplicationsForThatCandidate() {
        ApplicationEntity saved = persistApplication(ApplicationStatus.APPLIED);

        List<ApplicationEntity> results = applicationRepo.findByCandidateCandidateId(saved.getCandidate().getCandidateId());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getApplicationId()).isEqualTo(saved.getApplicationId());
    }

    @Test
    void findByStatus_returnsOnlyMatchingApplications() {
        persistApplication(ApplicationStatus.APPLIED);
        persistApplication(ApplicationStatus.HIRED);

        List<ApplicationEntity> applied = applicationRepo.findByStatus(ApplicationStatus.APPLIED);

        assertThat(applied).hasSize(1);
        assertThat(applied.get(0).getStatus()).isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    void findByJobJobIdAndCandidateCandidateId_whenExists_returnsIt() {
        ApplicationEntity saved = persistApplication(ApplicationStatus.APPLIED);

        Optional<ApplicationEntity> result = applicationRepo.findByJobJobIdAndCandidateCandidateId(
                saved.getJob().getJobId(), saved.getCandidate().getCandidateId());

        assertThat(result).isPresent();
        assertThat(result.get().getApplicationId()).isEqualTo(saved.getApplicationId());
    }

    @Test
    void findByJobJobIdAndCandidateCandidateId_whenNotExists_returnsEmpty() {
        Optional<ApplicationEntity> result = applicationRepo.findByJobJobIdAndCandidateCandidateId(
                "nonexistent-job", "nonexistent-candidate");

        assertThat(result).isEmpty();
    }

    @Test
    void existsByJobJobIdAndCandidateCandidateId_whenApplicationExists_returnsTrue() {
        ApplicationEntity saved = persistApplication(ApplicationStatus.APPLIED);

        boolean exists = applicationRepo.existsByJobJobIdAndCandidateCandidateId(
                saved.getJob().getJobId(), saved.getCandidate().getCandidateId());

        assertThat(exists).isTrue();
    }

    @Test
    void existsByJobJobIdAndCandidateCandidateId_whenNoApplication_returnsFalse() {
        boolean exists = applicationRepo.existsByJobJobIdAndCandidateCandidateId(
                "nonexistent-job", "nonexistent-candidate");

        assertThat(exists).isFalse();
    }
}