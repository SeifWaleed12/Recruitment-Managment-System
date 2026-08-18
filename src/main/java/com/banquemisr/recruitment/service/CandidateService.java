package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.cvparsing.model.BulkUploadJob;
import com.banquemisr.recruitment.cvparsing.model.BulkUploadProgressRespond;
import com.banquemisr.recruitment.cvparsing.model.ParsedCv;
import com.banquemisr.recruitment.cvparsing.service.BulkCvProcessingService;
import com.banquemisr.recruitment.cvparsing.service.CvParsingService;
import com.banquemisr.recruitment.data.entity.CandidateEntity;
import com.banquemisr.recruitment.data.entity.SkillEntity;
import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.repo.CandidateRepo;
import com.banquemisr.recruitment.data.repo.SkillRepo;
import com.banquemisr.recruitment.exception.DuplicateResourceException;
import com.banquemisr.recruitment.exception.ResourceNotFoundException;
import com.banquemisr.recruitment.mapper.CandidateMapper;
import com.banquemisr.recruitment.web.DTOs.request.CandidateRequest;
import com.banquemisr.recruitment.web.DTOs.respond.CandidateRespond;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateService {

    private static final int MAX_BULK_FILES = 20;

    private final CandidateRepo candidateRepo;
    private final SkillRepo skillRepo;
    private final UserService userService;
    private final CandidateMapper candidateMapper;
    private final CvParsingService cvParsingService;
    private final BulkCvProcessingService bulkCvProcessingService;

    /**
     * Single CV Upload & In-Memory Parsing.
     * Extracts name, email, phone, years of experience, and skills without retaining the physical file.
     */
    @Transactional
    public CandidateRespond parseAndSaveCandidate(MultipartFile file, String createdByUserId) {
        ParsedCv parsedCv = cvParsingService.parseInMemory(file);

        UserEntity creator = null;
        if (createdByUserId != null && !createdByUserId.isBlank()) {
            creator = userService.getUserEntityById(createdByUserId);
        }

        String email = parsedCv.getEmail();
        if (candidateRepo.existsByEmail(email)) {
            email = resolveUniqueEmail(email);
        }

        Set<SkillEntity> skillEntities = resolveSkillEntities(parsedCv.getSkills());

        CandidateEntity entity = CandidateEntity.builder()
                .firstName(parsedCv.getFirstName())
                .lastName(parsedCv.getLastName())
                .email(email)
                .phone(parsedCv.getPhone())
                .yearsOfExperience(parsedCv.getYearsOfExperience())
                .cvOriginalFilename(file.getOriginalFilename())
                .cvFileType(file.getContentType())
                .cvFilePath(null) // Zero file retention
                .createdBy(creator)
                .skills(skillEntities)
                .build();

        CandidateEntity savedEntity = candidateRepo.save(entity);
        return candidateMapper.toRespond(savedEntity);
    }

    /**
     * Initiates asynchronous in-memory parsing for multiple CV files.
     */
    public BulkUploadProgressRespond startBulkCvUpload(List<MultipartFile> files, String createdByUserId) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files were provided for bulk upload.");
        }

        if (files.size() > MAX_BULK_FILES) {
            throw new IllegalArgumentException("Bulk upload cannot exceed " + MAX_BULK_FILES + " files per batch.");
        }

        List<BulkCvProcessingService.InMemoryFile> inMemoryFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                try {
                    inMemoryFiles.add(new BulkCvProcessingService.InMemoryFile(
                            file.getOriginalFilename(),
                            file.getContentType(),
                            file.getBytes()
                    ));
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read file content for: " + file.getOriginalFilename(), e);
                }
            }
        }

        if (inMemoryFiles.isEmpty()) {
            throw new IllegalArgumentException("All provided files are empty.");
        }

        BulkUploadJob job = bulkCvProcessingService.createJob(inMemoryFiles.size());
        bulkCvProcessingService.processBulkUploadAsync(job.getJobId(), inMemoryFiles, createdByUserId);

        return BulkUploadProgressRespond.fromJob(job);
    }

    /**
     * Polls the live status of an asynchronous bulk upload job.
     */
    public BulkUploadProgressRespond getBulkUploadProgress(String jobId) {
        BulkUploadJob job = bulkCvProcessingService.getJob(jobId);
        return BulkUploadProgressRespond.fromJob(job);
    }

    /**
     * Manual Candidate Creation
     */
    @Transactional
    public CandidateRespond createCandidate(CandidateRequest request) {
        if (candidateRepo.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Candidate with email " + request.getEmail() + " already exists!");
        }

        UserEntity creator = null;
        if (request.getCreatedByUserId() != null) {
            creator = userService.getUserEntityById(request.getCreatedByUserId());
        }

        CandidateEntity entity = candidateMapper.toEntity(request, creator);
        CandidateEntity savedEntity = candidateRepo.save(entity);

        return candidateMapper.toRespond(savedEntity);
    }

    @Transactional(readOnly = true)
    public CandidateRespond getCandidateById(String candidateId) {
        CandidateEntity entity = getCandidateEntityById(candidateId);
        return candidateMapper.toRespond(entity);
    }

    @Transactional(readOnly = true)
    public CandidateEntity getCandidateEntityById(String candidateId) {
        return candidateRepo.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with ID: " + candidateId));
    }

    @Transactional(readOnly = true)
    public List<CandidateRespond> getAllCandidates() {
        return candidateRepo.findAll().stream()
                .map(candidateMapper::toRespond)
                .collect(Collectors.toList());
    }

    @Transactional
    public CandidateRespond updateCandidate(String candidateId, CandidateRequest request) {
        CandidateEntity existingCandidate = getCandidateEntityById(candidateId);

        existingCandidate.setFirstName(request.getFirstName());
        existingCandidate.setLastName(request.getLastName());
        existingCandidate.setEmail(request.getEmail());
        existingCandidate.setPhone(request.getPhone());
        existingCandidate.setYearsOfExperience(request.getYearsOfExperience());

        CandidateEntity updatedEntity = candidateRepo.save(existingCandidate);
        return candidateMapper.toRespond(updatedEntity);
    }

    @Transactional
    public void deleteCandidate(String candidateId) {
        if (!candidateRepo.existsById(candidateId)) {
            throw new ResourceNotFoundException("Candidate not found with ID: " + candidateId);
        }
        candidateRepo.deleteById(candidateId);
    }

    private Set<SkillEntity> resolveSkillEntities(Set<String> skillNames) {
        Set<SkillEntity> result = new HashSet<>();
        if (skillNames == null || skillNames.isEmpty()) {
            return result;
        }

        for (String skillName : skillNames) {
            SkillEntity skillEntity = skillRepo.findByNameIgnoreCase(skillName)
                    .orElseGet(() -> skillRepo.save(SkillEntity.builder().name(skillName).build()));
            result.add(skillEntity);
        }
        return result;
    }

    private String resolveUniqueEmail(String email) {
        int atIndex = email.indexOf('@');
        String prefix = (atIndex != -1) ? email.substring(0, atIndex) : email;
        String domain = (atIndex != -1) ? email.substring(atIndex) : "@example.com";
        return prefix + "+" + System.currentTimeMillis() + domain;
    }
}