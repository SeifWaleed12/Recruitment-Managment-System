package com.banquemisr.recruitment.web.controller;

import com.banquemisr.recruitment.cvparsing.model.BulkUploadProgressRespond;
import com.banquemisr.recruitment.data.enums.ApplicationStatus;
import com.banquemisr.recruitment.data.specification.CandidateSearchCriteria;
import com.banquemisr.recruitment.service.CandidateService;
import com.banquemisr.recruitment.web.DTOs.request.CandidateRequest;
import com.banquemisr.recruitment.web.DTOs.respond.CandidateRespond;
import com.banquemisr.recruitment.web.DTOs.respond.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/candidates")
@RequiredArgsConstructor
public class CandidateController {

    private final CandidateService candidateService;

    /**
     * Get paginated candidates list.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'INTERVIEWER')")
    public PageResponse<CandidateRespond> getAllCandidates(
            @PageableDefault(size = 20, sort = "candidateId", direction = Sort.Direction.ASC) Pageable pageable) {
        return this.candidateService.getAllCandidates(pageable);
    }

    /**
     * Dynamic structured & free-text search across name, skills, tags, application status, and experience.
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'INTERVIEWER')")
    public PageResponse<CandidateRespond> searchCandidates(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "skills", required = false) List<String> skills,
            @RequestParam(name = "tags", required = false) List<String> tags,
            @RequestParam(name = "status", required = false) ApplicationStatus status,
            @RequestParam(name = "minExperience", required = false) Integer minExperience,
            @RequestParam(name = "maxExperience", required = false) Integer maxExperience,
            @PageableDefault(size = 20, sort = "candidateId", direction = Sort.Direction.ASC) Pageable pageable) {

        CandidateSearchCriteria criteria = CandidateSearchCriteria.builder()
                .name(name)
                .skills(skills)
                .tags(tags)
                .status(status)
                .minExperience(minExperience)
                .maxExperience(maxExperience)
                .build();

        return this.candidateService.searchCandidates(criteria, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'INTERVIEWER')")
    public CandidateRespond getCandidateById(@PathVariable(name = "id") String candidateId) {
        return this.candidateService.getCandidateById(candidateId);
    }

    /**
     * Single CV Upload & In-Memory Parsing Endpoint (Accepts PDF and DOCX).
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_HR')")
    public CandidateRespond uploadAndParseCv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "createdByUserId", required = true) String createdByUserId) {
        return this.candidateService.parseAndSaveCandidate(file, createdByUserId);
    }

    /**
     * Bulk CV Upload Endpoint (Accepts up to 20 PDF and DOCX files).
     * Processes asynchronously in-memory and returns a trackable job ID.
     */
    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public BulkUploadProgressRespond bulkUploadCvs(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(name = "createdByUserId", required = false) String createdByUserId) {
        return this.candidateService.startBulkCvUpload(files, createdByUserId);
    }

    /**
     * Poll status of an asynchronous bulk CV upload job.
     */
    @GetMapping("/bulk-upload/{jobId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public BulkUploadProgressRespond getBulkUploadStatus(@PathVariable("jobId") String jobId) {
        return this.candidateService.getBulkUploadProgress(jobId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public CandidateRespond createCandidate(@Valid @RequestBody CandidateRequest candidateRequest) {
        return this.candidateService.createCandidate(candidateRequest);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public CandidateRespond updateCandidate(
            @PathVariable(name = "id") String candidateId,
            @Valid @RequestBody CandidateRequest candidateRequest) {
        return this.candidateService.updateCandidate(candidateId, candidateRequest);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public void deleteCandidate(@PathVariable(name = "id") String candidateId) {
        this.candidateService.deleteCandidate(candidateId);
    }

    @PostMapping("/{candidateId}/skills/{skillId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public CandidateRespond assignSkill(
            @PathVariable(name= "candidateId") String candidateId,
            @PathVariable(name= "skillId") String skillId){
        return this.candidateService.assignSkill(candidateId,skillId);
    }

    @DeleteMapping("/{candidateId}/skills/{skillId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public CandidateRespond removeSkill(
            @PathVariable(name= "candidateId") String candidateId,
            @PathVariable(name= "skillId") String skillId){
        return this.candidateService.removeSkill(candidateId,skillId);
    }

    @PostMapping("/{candidateId}/tags/{tagId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public CandidateRespond assignTag(
            @PathVariable(name= "candidateId") String candidateId,
            @PathVariable(name= "tagId") String tagId){
        return this.candidateService.assignTag(candidateId,tagId);
    }

    @DeleteMapping("/{candidateId}/tags/{tagId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public CandidateRespond removeTag(
            @PathVariable(name= "candidateId") String candidateId,
            @PathVariable(name= "tagId") String tagId){
        return this.candidateService.removeTag(candidateId,tagId);
    }
}
