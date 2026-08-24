package com.banquemisr.recruitment.data.repo;

import com.banquemisr.recruitment.data.entity.JobEntity;
import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.enums.JobStatus;
import com.banquemisr.recruitment.data.enums.Role;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class JobRepoTest {

    @Autowired
    private JobRepo jobRepo;

    @Autowired
    private EntityManager entityManager;

    private UserEntity persistHr() {
        UserEntity hr = UserEntity.builder()
                .userEmail("hr-" + UUID.randomUUID() + "@example.com")
                .userFname("Sara")
                .userLname("Ahmed")
                .role(Role.ROLE_HR)
                .build();
        entityManager.persist(hr);
        return hr;
    }

    private JobEntity persistJob(UserEntity creator, JobStatus status, String department) {
        JobEntity job = JobEntity.builder()
                .title("Backend Engineer")
                .description("Build backend services.")
                .department(department)
                .location("Cairo")
                .status(status)
                .createdBy(creator)
                .build();
        entityManager.persist(job);
        entityManager.flush();
        return job;
    }

    @Test
    void findByStatus_returnsOnlyMatchingJobs() {
        UserEntity hr = persistHr();
        persistJob(hr, JobStatus.DRAFT, "Engineering");
        persistJob(hr, JobStatus.OPEN, "Engineering");

        List<JobEntity> draftJobs = jobRepo.findByStatus(JobStatus.DRAFT);

        assertThat(draftJobs).hasSize(1);
        assertThat(draftJobs.get(0).getStatus()).isEqualTo(JobStatus.DRAFT);
    }

    @Test
    void findByStatus_whenNoneMatch_returnsEmptyList() {
        UserEntity hr = persistHr();
        persistJob(hr, JobStatus.DRAFT, "Engineering");

        List<JobEntity> closedJobs = jobRepo.findByStatus(JobStatus.CLOSED);

        assertThat(closedJobs).isEmpty();
    }

    @Test
    void findByDepartment_returnsOnlyMatchingJobs() {
        UserEntity hr = persistHr();
        persistJob(hr, JobStatus.DRAFT, "Engineering");
        persistJob(hr, JobStatus.DRAFT, "Marketing");

        List<JobEntity> engineeringJobs = jobRepo.findByDepartment("Engineering");

        assertThat(engineeringJobs).hasSize(1);
        assertThat(engineeringJobs.get(0).getDepartment()).isEqualTo("Engineering");
    }

    @Test
    void findByDepartment_whenNoneMatch_returnsEmptyList() {
        UserEntity hr = persistHr();
        persistJob(hr, JobStatus.DRAFT, "Engineering");

        List<JobEntity> salesJobs = jobRepo.findByDepartment("Sales");

        assertThat(salesJobs).isEmpty();
    }

    @Test
    void findByCreatedByUserId_returnsJobsCreatedByThatUser() {
        UserEntity hr1 = persistHr();
        UserEntity hr2 = persistHr();
        persistJob(hr1, JobStatus.DRAFT, "Engineering");
        persistJob(hr2, JobStatus.DRAFT, "Marketing");

        List<JobEntity> hr1Jobs = jobRepo.findByCreatedByUserId(hr1.getUserId());

        assertThat(hr1Jobs).hasSize(1);
        assertThat(hr1Jobs.get(0).getCreatedBy().getUserId()).isEqualTo(hr1.getUserId());
    }

    @Test
    void findByCreatedByUserId_whenUserCreatedNothing_returnsEmptyList() {
        UserEntity hr1 = persistHr();
        UserEntity hr2 = persistHr();
        persistJob(hr1, JobStatus.DRAFT, "Engineering");

        List<JobEntity> hr2Jobs = jobRepo.findByCreatedByUserId(hr2.getUserId());

        assertThat(hr2Jobs).isEmpty();
    }
}