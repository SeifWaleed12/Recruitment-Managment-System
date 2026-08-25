package com.banquemisr.recruitment.data.entity;

import com.banquemisr.recruitment.data.enums.JobStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobEntityTest {

    @Test
    void builder_withoutStatus_defaultsToDraft() {
        JobEntity job = JobEntity.builder()
                .title("Backend Engineer")
                .description("Build and maintain backend services.")
                .department("Engineering")
                .location("Cairo")
                .build();

        assertThat(job.getStatus()).isEqualTo(JobStatus.DRAFT);
    }

    @Test
    void setStatus_changesStatusFreely_noRestrictionCurrentlyEnforced() {
        JobEntity job = JobEntity.builder()
                .title("Backend Engineer")
                .description("Build and maintain backend services.")
                .department("Engineering")
                .location("Cairo")
                .build();

        job.setStatus(JobStatus.CLOSED);

        assertThat(job.getStatus()).isEqualTo(JobStatus.CLOSED);
    }

    @Test
    void builder_setsAllFieldsCorrectly() {
        UserEntity creator = UserEntity.builder().userFname("Sara").userLname("Ahmed").build();

        JobEntity job = JobEntity.builder()
                .title("Backend Engineer")
                .description("Build and maintain backend services.")
                .department("Engineering")
                .location("Cairo")
                .status(JobStatus.OPEN)
                .createdBy(creator)
                .build();

        assertThat(job.getTitle()).isEqualTo("Backend Engineer");
        assertThat(job.getDescription()).isEqualTo("Build and maintain backend services.");
        assertThat(job.getDepartment()).isEqualTo("Engineering");
        assertThat(job.getLocation()).isEqualTo("Cairo");
        assertThat(job.getStatus()).isEqualTo(JobStatus.OPEN);
        assertThat(job.getCreatedBy()).isSameAs(creator);
    }
}