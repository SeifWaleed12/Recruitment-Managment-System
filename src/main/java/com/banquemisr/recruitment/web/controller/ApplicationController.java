package com.banquemisr.recruitment.web.controller;

import com.banquemisr.recruitment.data.enums.ApplicationStatus;
import com.banquemisr.recruitment.service.ApplicationService;
import com.banquemisr.recruitment.web.DTOs.request.ApplicationRequest;
import com.banquemisr.recruitment.web.DTOs.respond.ApplicationRespond;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationRespond createApplication(@Valid @RequestBody ApplicationRequest applicationRequest) {
        return this.applicationService.createApplication(applicationRequest);
    }

    @GetMapping("/{id}")
    public ApplicationRespond getApplicationById(@PathVariable(name = "id") String applicationId) {
        return this.applicationService.getApplicationById(applicationId);
    }

    @GetMapping("/job/{jobId}")
    public List<ApplicationRespond> getApplicationsByJobId(@PathVariable(name = "jobId") String jobId) {
        return this.applicationService.getApplicationsByJobId(jobId);
    }

    @GetMapping("/candidate/{candidateId}")
    public List<ApplicationRespond> getApplicationsByCandidateId(@PathVariable(name = "candidateId") String candidateId) {
        return this.applicationService.getApplicationsByCandidateId(candidateId);
    }

    @PatchMapping("/{id}/status")
    public ApplicationRespond updateApplicationStatus(
            @PathVariable(name = "id") String applicationId,
            @RequestParam(name = "status") ApplicationStatus status) {
        return this.applicationService.updateApplicationStatus(applicationId, status);
    }

    @PatchMapping("/{id}/assign")
    public ApplicationRespond assignRecruiter(
            @PathVariable(name = "id") String applicationId,
            @RequestParam(name = "recruiterUserId") String recruiterUserId) {
        return this.applicationService.assignRecruiter(applicationId, recruiterUserId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteApplication(@PathVariable(name = "id") String applicationId) {
        this.applicationService.deleteApplication(applicationId);
    }
}
