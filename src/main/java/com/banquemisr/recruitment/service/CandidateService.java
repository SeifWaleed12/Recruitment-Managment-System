package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.data.entity.CandidateEntity;
import com.banquemisr.recruitment.data.entity.SkillEntity;
import com.banquemisr.recruitment.data.entity.TagEntity;
import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.repo.CandidateRepo;
import com.banquemisr.recruitment.exception.DuplicateResourceException;
import com.banquemisr.recruitment.exception.ResourceNotFoundException;
import com.banquemisr.recruitment.mapper.CandidateMapper;
import com.banquemisr.recruitment.web.DTOs.request.CandidateRequest;
import com.banquemisr.recruitment.web.DTOs.respond.CandidateRespond;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidateService {

    private final CandidateRepo candidateRepo;
    private final UserService userService;
    private final CandidateMapper candidateMapper;
    private final SkillService skillService;
    private final TagService tagService;

    /**
     * CV Upload & Parsing Hook Method
     * Ready for you to plug in your custom parser class logic.
     */
    @Transactional
    public CandidateRespond parseAndSaveCandidate(MultipartFile file, String createdByUserId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded CV file cannot be empty");
        }

        UserEntity creator = null;
        if (createdByUserId != null) {
            creator = userService.getUserEntityById(createdByUserId);
        }

        // TODO: Plug in your custom CV parsing service here when ready!
        // e.g. ParsedCvData data = myCvParserService.parse(file);

        CandidateEntity entity = CandidateEntity.builder()
                .firstName("Pending")
                .lastName("Parsing")
                .email("candidate." + System.currentTimeMillis() + "@example.com")
                .cvOriginalFilename(file.getOriginalFilename())
                .cvFileType(file.getContentType())
                .createdBy(creator)
                .build();

        CandidateEntity savedEntity = candidateRepo.save(entity);
        return candidateMapper.toRespond(savedEntity);
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

    @Transactional
    public CandidateRespond assignSkill(String candidateId, String skillId){
        CandidateEntity candidate= getCandidateEntityById(candidateId);
        SkillEntity skill= skillService.getSkillEntityById(skillId);
        candidate.getSkills().add(skill);
        return candidateMapper.toRespond(candidateRepo.save(candidate));
    }

    @Transactional
    public CandidateRespond removeSkill(String candidateId, String skillId) {
        CandidateEntity candidate = getCandidateEntityById(candidateId);
        candidate.getSkills().removeIf(s -> s.getSkillId().equals(skillId));
        return candidateMapper.toRespond(candidateRepo.save(candidate));
    }

    @Transactional
    public CandidateRespond assignTag(String candidateId, String tagId) {
        CandidateEntity candidate = getCandidateEntityById(candidateId);
        TagEntity tag = tagService.getTagEntityById(tagId);
        candidate.getTags().add(tag);
        return candidateMapper.toRespond(candidateRepo.save(candidate));
    }

    @Transactional
    public CandidateRespond removeTag(String candidateId, String tagId) {
        CandidateEntity candidate = getCandidateEntityById(candidateId);
        candidate.getTags().removeIf(t -> t.getTagId().equals(tagId));
        return candidateMapper.toRespond(candidateRepo.save(candidate));
    }
}