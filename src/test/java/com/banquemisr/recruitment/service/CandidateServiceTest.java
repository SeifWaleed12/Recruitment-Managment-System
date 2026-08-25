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
import com.banquemisr.recruitment.web.DTOs.respond.CandidateRespond;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;


import com.banquemisr.recruitment.data.entity.TagEntity;
import com.banquemisr.recruitment.web.DTOs.request.CandidateRequest;
import com.banquemisr.recruitment.web.DTOs.respond.PageResponse;
import com.banquemisr.recruitment.data.specification.CandidateSearchCriteria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidateServiceTest {

    @Mock
    private CandidateRepo candidateRepo;

    @Mock
    private SkillRepo skillRepo;

    @Mock
    private SkillService skillService;

    @Mock
    private TagService tagService;

    @Mock
    private UserService userService;

    @Mock
    private CandidateMapper candidateMapper;

    @Mock
    private CvParsingService cvParsingService;

    @Mock
    private BulkCvProcessingService bulkCvProcessingService;

    @InjectMocks
    private CandidateService candidateService;

    @Test
    void parseAndSaveCandidate_whenValidFile_savesAndReturnsRespond() {

        MultipartFile file = new MockMultipartFile(
                "cv",
                "resume.pdf",
                "application/pdf",
                "dummy".getBytes()
        );

        ParsedCv parsedCv = ParsedCv.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@test.com")
                .phone("123")
                .yearsOfExperience(5)
                .skills(Set.of("Java"))
                .build();

        UserEntity creator = UserEntity.builder()
                .userId("user-1")
                .build();

        SkillEntity skill = SkillEntity.builder()
                .name("Java")
                .build();

        CandidateEntity savedEntity = CandidateEntity.builder()
                .candidateId("cand-1")
                .build();

        CandidateRespond respond = CandidateRespond.builder()
                .candidateId("cand-1")
                .build();

        when(cvParsingService.parseInMemory(file)).thenReturn(parsedCv);
        when(userService.getUserEntityById("user-1")).thenReturn(creator);

        when(candidateRepo.existsByEmail("john@test.com")).thenReturn(false);

        when(skillRepo.findByNameIgnoreCase("Java"))
                .thenReturn(Optional.of(skill));

        when(candidateRepo.save(any(CandidateEntity.class)))
                .thenReturn(savedEntity);

        when(candidateMapper.toRespond(savedEntity))
                .thenReturn(respond);

        CandidateRespond result =
                candidateService.parseAndSaveCandidate(file, "user-1");

        assertThat(result.getCandidateId()).isEqualTo("cand-1");

        verify(candidateRepo).save(any(CandidateEntity.class));
    }

    @Test
    void parseAndSaveCandidate_whenEmailAlreadyExists_generatesUniqueEmail() {

        MultipartFile file = new MockMultipartFile(
                "cv",
                "resume.pdf",
                "application/pdf",
                "dummy".getBytes()
        );

        ParsedCv parsedCv = ParsedCv.builder()
                .email("john@test.com")
                .skills(Set.of())
                .build();

        CandidateEntity saved = CandidateEntity.builder()
                .candidateId("cand-1")
                .build();

        when(cvParsingService.parseInMemory(file))
                .thenReturn(parsedCv);

        when(candidateRepo.existsByEmail("john@test.com"))
                .thenReturn(true);

        when(candidateRepo.save(any()))
                .thenReturn(saved);

        when(candidateMapper.toRespond(saved))
                .thenReturn(CandidateRespond.builder()
                        .candidateId("cand-1")
                        .build());

        candidateService.parseAndSaveCandidate(file, null);

        verify(candidateRepo).save(any(CandidateEntity.class));
    }

    @Test
    void parseAndSaveCandidate_whenSkillDoesNotExist_createsSkill() {

        MultipartFile file = new MockMultipartFile(
                "cv",
                "resume.pdf",
                "application/pdf",
                "dummy".getBytes()
        );

        ParsedCv parsedCv = ParsedCv.builder()
                .email("john@test.com")
                .skills(Set.of("Spring"))
                .build();

        SkillEntity skill = SkillEntity.builder()
                .name("Spring")
                .build();

        when(cvParsingService.parseInMemory(file))
                .thenReturn(parsedCv);

        when(candidateRepo.existsByEmail(any()))
                .thenReturn(false);

        when(skillRepo.findByNameIgnoreCase("Spring"))
                .thenReturn(Optional.empty());

        when(skillRepo.save(any(SkillEntity.class)))
                .thenReturn(skill);

        when(candidateRepo.save(any()))
                .thenReturn(CandidateEntity.builder().build());

        when(candidateMapper.toRespond(any()))
                .thenReturn(CandidateRespond.builder().build());

        candidateService.parseAndSaveCandidate(file, null);

        verify(skillRepo).save(any(SkillEntity.class));
    }

    @Test
    void startBulkCvUpload_whenValidFiles_createsJobAndStartsProcessing() {

        MultipartFile file = new MockMultipartFile(
                "cv",
                "resume.pdf",
                "application/pdf",
                "dummy".getBytes()
        );

        BulkUploadJob job = new BulkUploadJob("job-1", 1);

        when(bulkCvProcessingService.createJob(1))
                .thenReturn(job);

        BulkUploadProgressRespond result =
                candidateService.startBulkCvUpload(List.of(file), "user-1");

        assertThat(result.getJobId()).isEqualTo("job-1");

        verify(bulkCvProcessingService)
                .processBulkUploadAsync(
                        eq("job-1"),
                        anyList(),
                        eq("user-1")
                );
    }

    @Test
    void startBulkCvUpload_whenNoFiles_throwsIllegalArgumentException() {

        assertThatThrownBy(() ->
                candidateService.startBulkCvUpload(List.of(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No files");
    }

    @Test
    void startBulkCvUpload_whenMoreThanTwentyFiles_throwsIllegalArgumentException() {

        List<MultipartFile> files =
                java.util.Collections.nCopies(
                        21,
                        new MockMultipartFile(
                                "cv",
                                "a.pdf",
                                "application/pdf",
                                "a".getBytes()
                        )
                );

        assertThatThrownBy(() ->
                candidateService.startBulkCvUpload(files, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("20");
    }

    @Test
    void startBulkCvUpload_whenAllFilesEmpty_throwsIllegalArgumentException() {

        MultipartFile file = new MockMultipartFile(
                "cv",
                "a.pdf",
                "application/pdf",
                new byte[0]
        );

        assertThatThrownBy(() ->
                candidateService.startBulkCvUpload(List.of(file), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("All provided files are empty");
    }

    @Test
    void getBulkUploadProgress_whenJobExists_returnsProgress() {

        BulkUploadJob job = new BulkUploadJob("job-1", 1);

        when(bulkCvProcessingService.getJob("job-1"))
                .thenReturn(job);

        BulkUploadProgressRespond result =
                candidateService.getBulkUploadProgress("job-1");

        assertThat(result.getJobId()).isEqualTo("job-1");
    }

    @Test
    void createCandidate_whenEmailDoesNotExist_savesAndReturnsRespond() {

        CandidateRequest request = CandidateRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@test.com")
                .createdByUserId("user-1")
                .build();

        UserEntity creator = UserEntity.builder()
                .userId("user-1")
                .build();

        CandidateEntity entity = CandidateEntity.builder()
                .candidateId("cand-1")
                .build();

        CandidateRespond respond = CandidateRespond.builder()
                .candidateId("cand-1")
                .build();

        when(candidateRepo.existsByEmail("john@test.com")).thenReturn(false);
        when(userService.getUserEntityById("user-1")).thenReturn(creator);
        when(candidateMapper.toEntity(request, creator)).thenReturn(entity);
        when(candidateRepo.save(entity)).thenReturn(entity);
        when(candidateMapper.toRespond(entity)).thenReturn(respond);

        CandidateRespond result = candidateService.createCandidate(request);

        assertThat(result.getCandidateId()).isEqualTo("cand-1");
        verify(candidateRepo).save(entity);
    }

    @Test
    void createCandidate_whenDuplicateEmail_throwsDuplicateResourceException() {

        CandidateRequest request = CandidateRequest.builder()
                .email("john@test.com")
                .build();

        when(candidateRepo.existsByEmail("john@test.com")).thenReturn(true);

        assertThatThrownBy(() -> candidateService.createCandidate(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(candidateRepo, never()).save(any());
    }

    @Test
    void getCandidateById_whenExists_returnsRespond() {

        CandidateEntity entity = CandidateEntity.builder()
                .candidateId("cand-1")
                .build();

        CandidateRespond respond = CandidateRespond.builder()
                .candidateId("cand-1")
                .build();

        when(candidateRepo.findById("cand-1"))
                .thenReturn(Optional.of(entity));

        when(candidateMapper.toRespond(entity))
                .thenReturn(respond);

        CandidateRespond result =
                candidateService.getCandidateById("cand-1");

        assertThat(result.getCandidateId()).isEqualTo("cand-1");
    }

    @Test
    void getCandidateById_whenNotFound_throwsResourceNotFoundException() {

        when(candidateRepo.findById("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                candidateService.getCandidateById("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getCandidateEntityById_whenExists_returnsEntity() {

        CandidateEntity entity = CandidateEntity.builder()
                .candidateId("cand-1")
                .build();

        when(candidateRepo.findById("cand-1"))
                .thenReturn(Optional.of(entity));

        CandidateEntity result =
                candidateService.getCandidateEntityById("cand-1");

        assertThat(result.getCandidateId()).isEqualTo("cand-1");
    }

    @Test
    void getCandidateEntityById_whenNotFound_throwsResourceNotFoundException() {

        when(candidateRepo.findById("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                candidateService.getCandidateEntityById("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void searchCandidates_returnsPageResponse() {

        CandidateSearchCriteria criteria = new CandidateSearchCriteria();

        Pageable pageable = PageRequest.of(0, 10);

        CandidateEntity entity = CandidateEntity.builder()
                .candidateId("cand-1")
                .build();

        CandidateRespond respond = CandidateRespond.builder()
                .candidateId("cand-1")
                .build();

        Page<CandidateEntity> entityPage =
                new PageImpl<>(List.of(entity));

        when(candidateRepo.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(entityPage);

        when(candidateMapper.toRespond(entity))
                .thenReturn(respond);

        PageResponse<CandidateRespond> result =
                candidateService.searchCandidates(criteria, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCandidateId())
                .isEqualTo("cand-1");
    }

    @Test
    void getAllCandidates_withPageable_returnsPageResponse() {

        Pageable pageable = PageRequest.of(0, 10);

        CandidateEntity entity = CandidateEntity.builder()
                .candidateId("cand-1")
                .build();

        CandidateRespond respond = CandidateRespond.builder()
                .candidateId("cand-1")
                .build();

        Page<CandidateEntity> page =
                new PageImpl<>(List.of(entity));

        when(candidateRepo.findAll(pageable))
                .thenReturn(page);

        when(candidateMapper.toRespond(entity))
                .thenReturn(respond);

        PageResponse<CandidateRespond> result =
                candidateService.getAllCandidates(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCandidateId())
                .isEqualTo("cand-1");
    }

    @Test
    void getAllCandidates_returnsList() {

        CandidateEntity entity = CandidateEntity.builder()
                .candidateId("cand-1")
                .build();

        CandidateRespond respond = CandidateRespond.builder()
                .candidateId("cand-1")
                .build();

        when(candidateRepo.findAllWithSkillsAndTags())
                .thenReturn(List.of(entity));

        when(candidateMapper.toRespond(entity))
                .thenReturn(respond);

        List<CandidateRespond> result =
                candidateService.getAllCandidates();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCandidateId())
                .isEqualTo("cand-1");
    }

    @Test
    void updateCandidate_whenCandidateExists_updatesAndReturnsRespond() {

        CandidateRequest request = CandidateRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@test.com")
                .phone("0123456789")
                .yearsOfExperience(7)
                .build();

        CandidateEntity entity = CandidateEntity.builder()
                .candidateId("cand-1")
                .firstName("John")
                .build();

        CandidateEntity updated = CandidateEntity.builder()
                .candidateId("cand-1")
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@test.com")
                .phone("0123456789")
                .yearsOfExperience(7)
                .build();

        CandidateRespond respond = CandidateRespond.builder()
                .candidateId("cand-1")
                .firstName("Jane")
                .build();

        when(candidateRepo.findById("cand-1"))
                .thenReturn(Optional.of(entity));

        when(candidateRepo.save(entity))
                .thenReturn(updated);

        when(candidateMapper.toRespond(updated))
                .thenReturn(respond);

        CandidateRespond result =
                candidateService.updateCandidate("cand-1", request);

        assertThat(entity.getFirstName()).isEqualTo("Jane");
        assertThat(entity.getLastName()).isEqualTo("Smith");
        assertThat(entity.getEmail()).isEqualTo("jane@test.com");

        assertThat(result.getCandidateId()).isEqualTo("cand-1");

        verify(candidateRepo).save(entity);
    }

    @Test
    void updateCandidate_whenCandidateDoesNotExist_throwsResourceNotFoundException() {

        CandidateRequest request = CandidateRequest.builder().build();

        when(candidateRepo.findById("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                candidateService.updateCandidate("missing", request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(candidateRepo, never()).save(any());
    }

    @Test
    void deleteCandidate_whenExists_deletesCandidate() {

        when(candidateRepo.existsById("cand-1"))
                .thenReturn(true);

        candidateService.deleteCandidate("cand-1");

        verify(candidateRepo).deleteById("cand-1");
    }

    @Test
    void deleteCandidate_whenNotFound_throwsResourceNotFoundException() {

        when(candidateRepo.existsById("missing"))
                .thenReturn(false);

        assertThatThrownBy(() ->
                candidateService.deleteCandidate("missing"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(candidateRepo, never()).deleteById(any());
    }

    @Test
    void assignSkill_addsSkillAndReturnsRespond() {

        CandidateEntity candidate = CandidateEntity.builder()
                .candidateId("cand-1")
                .skills(new HashSet<>())
                .build();

        SkillEntity skill = SkillEntity.builder()
                .id("skill-1")
                .name("Java")
                .build();

        CandidateRespond respond = CandidateRespond.builder()
                .candidateId("cand-1")
                .build();

        when(candidateRepo.findById("cand-1"))
                .thenReturn(Optional.of(candidate));

        when(skillService.getSkillEntityById("skill-1"))
                .thenReturn(skill);

        when(candidateRepo.save(candidate))
                .thenReturn(candidate);

        when(candidateMapper.toRespond(candidate))
                .thenReturn(respond);

        CandidateRespond result =
                candidateService.assignSkill("cand-1", "skill-1");

        assertThat(candidate.getSkills()).contains(skill);
        assertThat(result.getCandidateId()).isEqualTo("cand-1");
    }


    @Test
    void removeSkill_removesSkillAndReturnsRespond() {

        SkillEntity skill = SkillEntity.builder()
                .id("skill-1")
                .name("Java")
                .build();

        CandidateEntity candidate = CandidateEntity.builder()
                .candidateId("cand-1")
                .skills(new HashSet<>(Set.of(skill)))
                .build();

        CandidateRespond respond = CandidateRespond.builder()
                .candidateId("cand-1")
                .build();

        when(candidateRepo.findById("cand-1"))
                .thenReturn(Optional.of(candidate));

        when(skillService.getSkillEntityById("skill-1"))
                .thenReturn(skill);

        when(candidateRepo.save(candidate))
                .thenReturn(candidate);

        when(candidateMapper.toRespond(candidate))
                .thenReturn(respond);

        candidateService.removeSkill("cand-1", "skill-1");

        assertThat(candidate.getSkills()).doesNotContain(skill);
    }

    @Test
    void assignTag_addsTagAndReturnsRespond() {

        CandidateEntity candidate = CandidateEntity.builder()
                .candidateId("cand-1")
                .tags(new HashSet<>())
                .build();

        TagEntity tag = TagEntity.builder()
                .tagId("tag-1")
                .name("Backend")
                .build();

        CandidateRespond respond = CandidateRespond.builder()
                .candidateId("cand-1")
                .build();

        when(candidateRepo.findById("cand-1"))
                .thenReturn(Optional.of(candidate));

        when(tagService.getTagEntityById("tag-1"))
                .thenReturn(tag);

        when(candidateRepo.save(candidate))
                .thenReturn(candidate);

        when(candidateMapper.toRespond(candidate))
                .thenReturn(respond);

        CandidateRespond result =
                candidateService.assignTag("cand-1", "tag-1");

        assertThat(candidate.getTags()).contains(tag);
        assertThat(result.getCandidateId()).isEqualTo("cand-1");
    }


    @Test
    void removeTag_removesTagAndReturnsRespond() {

        TagEntity tag = TagEntity.builder()
                .tagId("tag-1")
                .name("Backend")
                .build();

        CandidateEntity candidate = CandidateEntity.builder()
                .candidateId("cand-1")
                .tags(new HashSet<>(Set.of(tag)))
                .build();

        CandidateRespond respond = CandidateRespond.builder()
                .candidateId("cand-1")
                .build();

        when(candidateRepo.findById("cand-1"))
                .thenReturn(Optional.of(candidate));

        when(tagService.getTagEntityById("tag-1"))
                .thenReturn(tag);

        when(candidateRepo.save(candidate))
                .thenReturn(candidate);

        when(candidateMapper.toRespond(candidate))
                .thenReturn(respond);

        candidateService.removeTag("cand-1", "tag-1");

        assertThat(candidate.getTags()).doesNotContain(tag);
    }
}
