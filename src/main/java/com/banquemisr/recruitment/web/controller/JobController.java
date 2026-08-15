package com.banquemisr.recruitment.web.controller;


import com.banquemisr.recruitment.data.enums.JobStatus;
import com.banquemisr.recruitment.service.JobService;
import com.banquemisr.recruitment.web.DTOs.request.JobRequest;
import com.banquemisr.recruitment.web.DTOs.respond.JobRespond;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;


    @GetMapping
    public List<JobRespond> getAllJobs(@RequestParam(name="status",required = false)JobStatus status){
        if (status != null) {
            return this.jobService.getJobsByStatus(status);
        }
        return this.jobService.getAllJobs();
    }

    @GetMapping("/{id}")
    public JobRespond getJobById(@PathVariable(name="id") String jobId){
        return this.jobService.getJobById(jobId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobRespond createJob(@Valid@RequestBody JobRequest jobRequest){
        return this.jobService.createJob(jobRequest);
    }

    @PutMapping("/{id}")
    public JobRespond updateJob(@PathVariable(name ="id") String jobId, @Valid @RequestBody JobRequest jobRequest){
        return this.jobService.updateJob(jobId,jobRequest);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteJob(@PathVariable(name = "id") String jobId){
        this.jobService.deleteJob(jobId);
    }

    @PatchMapping("/{id}/status")
    public JobRespond changeStatus(@PathVariable(name="id")String jobId,@RequestParam(name = "status") JobStatus status){
        return this.jobService.updateJobStatus(jobId,status);
    }




}
