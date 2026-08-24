package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.data.entity.JobEntity;
import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.enums.JobStatus;
import com.banquemisr.recruitment.data.repo.JobRepo;
import com.banquemisr.recruitment.exception.ResourceNotFoundException;
import com.banquemisr.recruitment.mapper.JobMapper;
import com.banquemisr.recruitment.web.DTOs.request.JobRequest;
import com.banquemisr.recruitment.web.DTOs.respond.JobRespond;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepo jobRepo;

    @Mock
    private UserService userService;

    @Mock
    private JobMapper jobMapper;

    @InjectMocks
    private JobService jobService;

    @Test
    void createJob_savesAndReturnsRespond() {

        JobRequest request = JobRequest.builder()
                .createdByUserId("user-1")
                .build();

        UserEntity creator = UserEntity.builder()
                .userId("user-1")
                .build();

        JobEntity entity = JobEntity.builder()
                .jobId("job-1")
                .build();

        JobRespond respond = JobRespond.builder()
                .jobId("job-1")
                .build();

        when(userService.getUserEntityById("user-1")).thenReturn(creator);
        when(jobMapper.toEntity(request, creator)).thenReturn(entity);
        when(jobRepo.save(entity)).thenReturn(entity);
        when(jobMapper.toRespond(entity)).thenReturn(respond);

        JobRespond result = jobService.createJob(request);

        assertThat(result.getJobId()).isEqualTo("job-1");
        verify(jobRepo).save(entity);
    }

    @Test
    void getJobById_whenExists_returnsRespond() {

        JobEntity entity = JobEntity.builder()
                .jobId("job-1")
                .build();

        JobRespond respond = JobRespond.builder()
                .jobId("job-1")
                .build();

        when(jobRepo.findById("job-1")).thenReturn(Optional.of(entity));
        when(jobMapper.toRespond(entity)).thenReturn(respond);

        JobRespond result = jobService.getJobById("job-1");

        assertThat(result.getJobId()).isEqualTo("job-1");
    }

    @Test
    void getJobById_whenNotFound_throwsResourceNotFoundException() {

        when(jobRepo.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getJobById("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllJobs_returnsList() {

        JobEntity entity = JobEntity.builder()
                .jobId("job-1")
                .build();

        JobRespond respond = JobRespond.builder()
                .jobId("job-1")
                .build();

        when(jobRepo.findAll()).thenReturn(List.of(entity));
        when(jobMapper.toRespond(entity)).thenReturn(respond);

        List<JobRespond> result = jobService.getAllJobs();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getJobId()).isEqualTo("job-1");
    }

    @Test
    void getJobsByStatus_returnsMatchingJobs() {

        JobEntity entity = JobEntity.builder()
                .jobId("job-1")
                .status(JobStatus.OPEN)
                .build();

        JobRespond respond = JobRespond.builder()
                .jobId("job-1")
                .build();

        when(jobRepo.findByStatus(JobStatus.OPEN)).thenReturn(List.of(entity));
        when(jobMapper.toRespond(entity)).thenReturn(respond);

        List<JobRespond> result = jobService.getJobsByStatus(JobStatus.OPEN);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getJobId()).isEqualTo("job-1");
    }

    @Test
    void updateJob_whenExists_updatesAndReturnsRespond() {

        JobEntity entity = JobEntity.builder()
                .jobId("job-1")
                .title("Old")
                .build();

        JobRequest request = JobRequest.builder()
                .title("New")
                .description("Description")
                .department("IT")
                .location("Cairo")
                .status(JobStatus.OPEN)
                .build();

        JobRespond respond = JobRespond.builder()
                .jobId("job-1")
                .build();

        when(jobRepo.findById("job-1")).thenReturn(Optional.of(entity));
        when(jobRepo.save(entity)).thenReturn(entity);
        when(jobMapper.toRespond(entity)).thenReturn(respond);

        JobRespond result = jobService.updateJob("job-1", request);

        assertThat(entity.getTitle()).isEqualTo("New");
        assertThat(entity.getDescription()).isEqualTo("Description");
        assertThat(entity.getDepartment()).isEqualTo("IT");
        assertThat(entity.getLocation()).isEqualTo("Cairo");
        assertThat(entity.getStatus()).isEqualTo(JobStatus.OPEN);

        assertThat(result.getJobId()).isEqualTo("job-1");
    }

    @Test
    void updateJob_whenNotFound_throwsResourceNotFoundException() {

        when(jobRepo.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                jobService.updateJob("missing", JobRequest.builder().build()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(jobRepo, never()).save(any());
    }

    @Test
    void updateJobStatus_updatesStatusAndReturnsRespond() {

        JobEntity entity = JobEntity.builder()
                .jobId("job-1")
                .status(JobStatus.OPEN)
                .build();

        JobRespond respond = JobRespond.builder()
                .jobId("job-1")
                .build();

        when(jobRepo.findById("job-1")).thenReturn(Optional.of(entity));
        when(jobRepo.save(entity)).thenReturn(entity);
        when(jobMapper.toRespond(entity)).thenReturn(respond);

        JobRespond result =
                jobService.updateJobStatus("job-1", JobStatus.CLOSED);

        assertThat(entity.getStatus()).isEqualTo(JobStatus.CLOSED);
        assertThat(result.getJobId()).isEqualTo("job-1");
    }

    @Test
    void updateJobStatus_whenNotFound_throwsResourceNotFoundException() {

        when(jobRepo.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                jobService.updateJobStatus("missing", JobStatus.OPEN))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(jobRepo, never()).save(any());
    }

    @Test
    void deleteJob_whenExists_deletesJob() {

        when(jobRepo.existsById("job-1")).thenReturn(true);

        jobService.deleteJob("job-1");

        verify(jobRepo).deleteById("job-1");
    }

    @Test
    void deleteJob_whenNotFound_throwsResourceNotFoundException() {

        when(jobRepo.existsById("missing")).thenReturn(false);

        assertThatThrownBy(() -> jobService.deleteJob("missing"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(jobRepo, never()).deleteById(any());
    }

    @Test
    void getJobEntityById_whenExists_returnsEntity() {

        JobEntity entity = JobEntity.builder()
                .jobId("job-1")
                .build();

        when(jobRepo.findById("job-1")).thenReturn(Optional.of(entity));

        JobEntity result = jobService.getJobEntityById("job-1");

        assertThat(result.getJobId()).isEqualTo("job-1");
    }

    @Test
    void getJobEntityById_whenNotFound_throwsResourceNotFoundException() {

        when(jobRepo.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                jobService.getJobEntityById("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}