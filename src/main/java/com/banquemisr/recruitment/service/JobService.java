package com.banquemisr.recruitment.service;


import com.banquemisr.recruitment.data.entity.JobEntity;
import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.enums.JobStatus;
import com.banquemisr.recruitment.data.repo.JobRepo;
import com.banquemisr.recruitment.exception.ResourceNotFoundException;
import com.banquemisr.recruitment.mapper.JobMapper;
import com.banquemisr.recruitment.web.DTOs.request.JobRequest;
import com.banquemisr.recruitment.web.DTOs.respond.JobRespond;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class JobService {

    private final JobRepo jobRepo;
    private final UserService userService;
    private final JobMapper jobMapper;

    public JobService(JobRepo jobRepo, UserService userService, JobMapper jobMapper) {
        this.jobRepo = jobRepo;
        this.userService = userService;
        this.jobMapper = jobMapper;
    }

    public JobRespond createRoom(JobRequest jobDTO){

        UserEntity creator = userService.getUserEntityById(jobDTO.getCreatedByUserId());

        JobEntity entity = jobMapper.toEntity(jobDTO);
        JobEntity savedEntity = jobRepo.save(entity);

        return jobMapper.toRespond(savedEntity);
    }


    public JobRespond getJobById(String jobId) {
        JobEntity entity = jobRepo.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + jobId));
        return jobMapper.toRespond(entity);
    }

    public List<JobRespond> getAllJobs() {
        return jobRepo.findAll().stream()
                .map(jobMapper::toRespond)
                .collect(Collectors.toList());
    }

    public List<JobRespond> getJobsByStatus(JobStatus status) {
        return jobRepo.findByStatus(status).stream()
                .map(jobMapper::toRespond)
                .collect(Collectors.toList());
    }

    public JobRespond updateJob(String jobId, JobRequest request) {
        JobEntity existingJob = jobRepo.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + jobId));
        existingJob.setTitle(request.getTitle());
        existingJob.setDescription(request.getDescription());
        existingJob.setDepartment(request.getDepartment());
        existingJob.setLocation(request.getLocation());
        if (request.getStatus() != null) {
            existingJob.setStatus(request.getStatus());
        }
        JobEntity updatedJob = jobRepo.save(existingJob);
        return jobMapper.toRespond(updatedJob);
    }

    public JobRespond updateJobStatus(String jobId, JobStatus status) {
        JobEntity existingJob = jobRepo.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + jobId));
        existingJob.setStatus(status);
        JobEntity updatedJob = jobRepo.save(existingJob);
        return jobMapper.toRespond(updatedJob);
    }

    public void deleteJob(String jobId) {
        if (!jobRepo.existsById(jobId)) {
            throw new ResourceNotFoundException("Job not found with ID: " + jobId);
        }
        jobRepo.deleteById(jobId);
    }

    public JobEntity getJobEntityById(String jobId) {
        return jobRepo.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + jobId));
    }

}
