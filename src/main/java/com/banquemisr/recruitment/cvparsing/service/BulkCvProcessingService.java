package com.banquemisr.recruitment.cvparsing.service;

import com.banquemisr.recruitment.cvparsing.extractor.CvDataExtractor;
import com.banquemisr.recruitment.cvparsing.model.BulkUploadJob;
import com.banquemisr.recruitment.cvparsing.model.ParsedCv;
import com.banquemisr.recruitment.cvparsing.parser.CvParserRegistry;
import com.banquemisr.recruitment.data.entity.CandidateEntity;
import com.banquemisr.recruitment.data.entity.SkillEntity;
import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.repo.CandidateRepo;
import com.banquemisr.recruitment.data.repo.SkillRepo;
import com.banquemisr.recruitment.data.repo.UserRepo;
import com.banquemisr.recruitment.exception.ResourceNotFoundException;
import com.banquemisr.recruitment.mapper.CandidateMapper;
import com.banquemisr.recruitment.web.DTOs.respond.CandidateRespond;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkCvProcessingService {

    public record InMemoryFile(String filename, String contentType, byte[] bytes) {}

    private final Map<String, BulkUploadJob> jobRegistry = new ConcurrentHashMap<>();

    private final CvParserRegistry parserRegistry;
    private final CvDataExtractor dataExtractor;
    private final CandidateRepo candidateRepo;
    private final SkillRepo skillRepo;
    private final UserRepo userRepo;
    private final CandidateMapper candidateMapper;

    public BulkUploadJob createJob(int totalFiles) {
        String jobId = UUID.randomUUID().toString();
        BulkUploadJob job = new BulkUploadJob(jobId, totalFiles);
        jobRegistry.put(jobId, job);
        return job;
    }

    public BulkUploadJob getJob(String jobId) {
        BulkUploadJob job = jobRegistry.get(jobId);
        if (job == null) {
            throw new ResourceNotFoundException("Bulk upload job not found with ID: " + jobId);
        }
        return job;
    }

    /**
     * Executes bulk CV parsing asynchronously across a bounded thread pool.
     * All files are parsed in-memory from memory buffers with zero disk retention.
     */
    @Async("cvBulkAsyncExecutor")
    public void processBulkUploadAsync(String jobId, List<InMemoryFile> files, String createdByUserId) {
        BulkUploadJob job = getJob(jobId);
        job.setStatus(BulkUploadJob.JobStatus.IN_PROGRESS);

        log.info("Starting asynchronous bulk parsing for Job '{}' ({} files)", jobId, files.size());

        UserEntity creator = null;
        if (createdByUserId != null) {
            creator = userRepo.findById(createdByUserId).orElse(null);
        }

        for (InMemoryFile file : files) {
            try {
                CandidateRespond candidateRespond = processSingleInMemoryFile(file, creator);
                job.addSuccess(candidateRespond);
            } catch (Exception e) {
                String error = "File '" + file.filename() + "': " + e.getMessage();
                log.warn("Error processing file in bulk job '{}': {}", jobId, error);
                job.addError(error);
            }
        }

        job.setCompletedAt(Instant.now());
        if (job.getSuccessfulFiles().get() > 0) {
            job.setStatus(BulkUploadJob.JobStatus.COMPLETED);
        } else {
            job.setStatus(BulkUploadJob.JobStatus.FAILED);
        }

        log.info("Completed asynchronous bulk parsing for Job '{}'. Success: {}, Failed: {}",
                jobId, job.getSuccessfulFiles().get(), job.getFailedFiles().get());
    }

    @Transactional
    public CandidateRespond processSingleInMemoryFile(InMemoryFile file, UserEntity creator) throws Exception {
        if (file.bytes() == null || file.bytes().length == 0) {
            throw new IllegalArgumentException("File content cannot be empty");
        }

        // 1. Extract text in-memory via registered strategy
        String rawText;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(file.bytes())) {
            rawText = parserRegistry.parseToText(bais, file.contentType(), file.filename());
        }

        // 2. Extract structured candidate fields
        ParsedCv parsedCv = dataExtractor.extract(rawText, file.filename());

        // 3. Resolve unique email
        String email = parsedCv.getEmail();
        if (candidateRepo.existsByEmail(email)) {
            email = resolveUniqueEmail(email);
        }

        // 4. Resolve and save skills
        Set<SkillEntity> skillEntities = resolveSkillEntities(parsedCv.getSkills());

        // 5. Build and save CandidateEntity (Zero retention: no file path stored)
        CandidateEntity entity = CandidateEntity.builder()
                .firstName(parsedCv.getFirstName())
                .lastName(parsedCv.getLastName())
                .email(email)
                .phone(parsedCv.getPhone())
                .yearsOfExperience(parsedCv.getYearsOfExperience())
                .cvOriginalFilename(file.filename())
                .cvFileType(file.contentType())
                .cvFilePath(null) // Zero retention: nothing stored on disk
                .createdBy(creator)
                .skills(skillEntities)
                .build();

        CandidateEntity savedEntity = candidateRepo.save(entity);
        return candidateMapper.toRespond(savedEntity);
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
