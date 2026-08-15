package com.banquemisr.recruitment.web.controller;

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

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CandidateRespond uploadAndParseCv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "createdByUserId", required = false) String createdByUserId) {
        return this.candidateService.parseAndSaveCandidate(file, createdByUserId);
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
}
