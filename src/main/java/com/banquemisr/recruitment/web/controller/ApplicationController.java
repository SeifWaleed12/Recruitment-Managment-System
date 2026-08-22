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

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ApplicationRespond createApplication(@Valid @RequestBody ApplicationRequest applicationRequest) {
        return this.applicationService.createApplication(applicationRequest);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'INTERVIEWER')")
    public ApplicationRespond getApplicationById(@PathVariable(name = "id") String applicationId) {
        return this.applicationService.getApplicationById(applicationId);
    }

    @GetMapping("/job/{jobId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'INTERVIEWER')")
    public List<ApplicationRespond> getApplicationsByJobId(@PathVariable(name = "jobId") String jobId) {
        return this.applicationService.getApplicationsByJobId(jobId);
    }

    @GetMapping("/candidate/{candidateId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'INTERVIEWER')")
    public List<ApplicationRespond> getApplicationsByCandidateId(@PathVariable(name = "candidateId") String candidateId) {
        return this.applicationService.getApplicationsByCandidateId(candidateId);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ApplicationRespond updateApplicationStatus(
            @PathVariable(name = "id") String applicationId,
            @RequestParam(name = "status") ApplicationStatus status) {
        return this.applicationService.updateApplicationStatus(applicationId, status);
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ApplicationRespond assignRecruiter(
            @PathVariable(name = "id") String applicationId,
            @RequestParam(name = "recruiterUserId") String recruiterUserId) {
        return this.applicationService.assignRecruiter(applicationId, recruiterUserId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public void deleteApplication(@PathVariable(name = "id") String applicationId) {
        this.applicationService.deleteApplication(applicationId);
    }
}
