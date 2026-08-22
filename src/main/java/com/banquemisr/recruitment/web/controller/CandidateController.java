package com.banquemisr.recruitment.web.controller;

import com.banquemisr.recruitment.cvparsing.model.BulkUploadProgressRespond;
import com.banquemisr.recruitment.service.CandidateService;
import com.banquemisr.recruitment.web.DTOs.request.CandidateRequest;
import com.banquemisr.recruitment.web.DTOs.respond.CandidateRespond;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/candidates")
@RequiredArgsConstructor
public class CandidateController {

    private final CandidateService candidateService;

    @GetMapping
    public List<CandidateRespond> getAllCandidates() {
        return this.candidateService.getAllCandidates();
    }

    @GetMapping("/{id}")
    public CandidateRespond getCandidateById(@PathVariable(name = "id") String candidateId) {
        return this.candidateService.getCandidateById(candidateId);
    }

    /**
     * Single CV Upload & In-Memory Parsing Endpoint (Accepts PDF and DOCX).
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CandidateRespond uploadAndParseCv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "createdByUserId", required = false) String createdByUserId) {
        return this.candidateService.parseAndSaveCandidate(file, createdByUserId);
    }

    /**
     * Bulk CV Upload Endpoint (Accepts up to 20 PDF and DOCX files).
     * Processes asynchronously in-memory and returns a trackable job ID.
     */
    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public BulkUploadProgressRespond bulkUploadCvs(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(name = "createdByUserId", required = false) String createdByUserId) {
        return this.candidateService.startBulkCvUpload(files, createdByUserId);
    }

    /**
     * Poll status of an asynchronous bulk CV upload job.
     */
    @GetMapping("/bulk-upload/{jobId}/status")
    public BulkUploadProgressRespond getBulkUploadStatus(@PathVariable("jobId") String jobId) {
        return this.candidateService.getBulkUploadProgress(jobId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CandidateRespond createCandidate(@Valid @RequestBody CandidateRequest candidateRequest) {
        return this.candidateService.createCandidate(candidateRequest);
    }

    @PutMapping("/{id}")
    public CandidateRespond updateCandidate(
            @PathVariable(name = "id") String candidateId,
            @Valid @RequestBody CandidateRequest candidateRequest) {
        return this.candidateService.updateCandidate(candidateId, candidateRequest);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCandidate(@PathVariable(name = "id") String candidateId) {
        this.candidateService.deleteCandidate(candidateId);
    }

    @PostMapping("/{candidateId}/skills/{skillId}")
    public CandidateRespond assignSkill(
            @PathVariable(name= "candidateId") String candidateId,
            @PathVariable(name= "skillId") String skillId){
        return this.candidateService.assignSkill(candidateId,skillId);
    }

    @DeleteMapping("/{candidateId}/skills/{skillId}")
    public CandidateRespond removeSkill(
            @PathVariable(name= "candidateId") String candidateId,
            @PathVariable(name= "skillId") String skillId){
        return this.candidateService.removeSkill(candidateId,skillId);
    }

    @PostMapping("/{candidateId}/tags/{tagId}")
    public CandidateRespond assignTag(
            @PathVariable(name= "candidateId") String candidateId,
            @PathVariable(name= "tagId") String tagId){
        return this.candidateService.assignTag(candidateId,tagId);
    }

    @DeleteMapping("/{candidateId}/tags/{tagId}")
    public CandidateRespond removeTag(
            @PathVariable(name= "candidateId") String candidateId,
            @PathVariable(name= "tagId") String tagId){
        return this.candidateService.removeTag(candidateId,tagId);
    }
}
