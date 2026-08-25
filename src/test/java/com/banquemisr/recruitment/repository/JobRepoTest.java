package com.banquemisr.recruitment.repository;

import com.banquemisr.recruitment.data.entity.JobEntity;
import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.enums.JobStatus;
import com.banquemisr.recruitment.data.enums.Role;
import com.banquemisr.recruitment.data.repo.JobRepo;
import com.banquemisr.recruitment.data.repo.UserRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class JobRepoTest {

    @Autowired
    private JobRepo jobRepo;

    @Autowired
    private UserRepo userRepo;

    @Test
    @DisplayName("findByStatus - should return only jobs matching specified status")
    void shouldFindJobsByStatus() {
        UserEntity creator = userRepo.save(UserEntity.builder()
                .userEmail("repo.creator@banquemisr.com")
                .userFname("Repo")
                .userLname("User")
                .role(Role.ROLE_HR)
                .enabled(true)
                .build());

        JobEntity openJob = JobEntity.builder()
                .title("Cloud Architect")
                .description("AWS / Azure expert")
                .department("Infrastructure")
                .location("Cairo")
                .status(JobStatus.OPEN)
                .createdBy(creator)
                .build();

        JobEntity closedJob = JobEntity.builder()
                .title("Scrum Master")
                .description("Agile coach")
                .department("PMO")
                .location("Cairo")
                .status(JobStatus.CLOSED)
                .createdBy(creator)
                .build();

        jobRepo.saveAll(List.of(openJob, closedJob));

        List<JobEntity> openJobs = jobRepo.findByStatus(JobStatus.OPEN);

        assertThat(openJobs).hasSize(1);
        assertThat(openJobs.get(0).getTitle()).isEqualTo("Cloud Architect");
    }
}
