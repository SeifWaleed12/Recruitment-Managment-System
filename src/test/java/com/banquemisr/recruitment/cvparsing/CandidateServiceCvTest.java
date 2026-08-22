package com.banquemisr.recruitment.cvparsing;

import com.banquemisr.recruitment.cvparsing.model.BulkUploadJob;
import com.banquemisr.recruitment.cvparsing.model.BulkUploadProgressRespond;
import com.banquemisr.recruitment.cvparsing.model.ParsedCv;
import com.banquemisr.recruitment.cvparsing.service.BulkCvProcessingService;
import com.banquemisr.recruitment.cvparsing.service.CvParsingService;
import com.banquemisr.recruitment.data.entity.CandidateEntity;
import com.banquemisr.recruitment.data.entity.SkillEntity;
import com.banquemisr.recruitment.data.repo.CandidateRepo;
import com.banquemisr.recruitment.data.repo.SkillRepo;
import com.banquemisr.recruitment.mapper.CandidateMapper;
import com.banquemisr.recruitment.mapper.TagMapper;
import com.banquemisr.recruitment.service.CandidateService;
import com.banquemisr.recruitment.service.SkillService;
import com.banquemisr.recruitment.service.TagService;
import com.banquemisr.recruitment.service.UserService;
import com.banquemisr.recruitment.web.DTOs.respond.CandidateRespond;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidateServiceCvTest {

    @Mock
    private CandidateRepo candidateRepo;

    @Mock
    private SkillRepo skillRepo;

    @Mock
    private SkillService skillService;

    @Mock
    private TagService tagService;

    @Mock
    private SkillService skillService;
    @Mock
    private TagService tagService;
    @Mock
    private UserService userService;

    @Mock
    private CvParsingService cvParsingService;

    @Mock
    private BulkCvProcessingService bulkCvProcessingService;

    private CandidateMapper candidateMapper;
    private TagMapper tagMapper;
    private CandidateService candidateService;

    @BeforeEach
    void setUp() {
        tagMapper = new TagMapper();
        candidateMapper = new CandidateMapper(tagMapper);

        candidateService = new CandidateService(
                candidateRepo,
                skillRepo,
                skillService,
                tagService,
                userService,
                candidateMapper,
                cvParsingService,
                bulkCvProcessingService
        );
    }

    @Test
    @DisplayName("Should parse single CV in-memory and save candidate with extracted skills")
    void testParseAndSaveCandidate() {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Ahmed_Ali_CV.pdf",
                "application/pdf",
                "Sample PDF content".getBytes()
        );

        ParsedCv parsedCv = ParsedCv.builder()
                .firstName("Ahmed")
                .lastName("Ali")
                .email("ahmed.ali@example.com")
                .phone("+201012345678")
                .yearsOfExperience(5)
                .skills(Set.of("Java", "Spring Boot", "Docker"))
                .build();

        when(cvParsingService.parseInMemory(file))
                .thenReturn(parsedCv);

        when(candidateRepo.existsByEmail("ahmed.ali@example.com"))
                .thenReturn(false);

        SkillEntity javaSkill = SkillEntity.builder()
                .id("1")
                .name("Java")
                .build();

        SkillEntity springSkill = SkillEntity.builder()
                .id("2")
                .name("Spring Boot")
                .build();

        SkillEntity dockerSkill = SkillEntity.builder()
                .id("3")
                .name("Docker")
                .build();

        when(skillRepo.findByNameIgnoreCase("Java"))
                .thenReturn(Optional.of(javaSkill));

        when(skillRepo.findByNameIgnoreCase("Spring Boot"))
                .thenReturn(Optional.of(springSkill));

        when(skillRepo.findByNameIgnoreCase("Docker"))
                .thenReturn(Optional.of(dockerSkill));

        when(candidateRepo.save(any(CandidateEntity.class)))
                .thenAnswer(invocation -> {
                    CandidateEntity entity = invocation.getArgument(0);
                    entity.setCandidateId("cand-123");
                    return entity;
                });

        CandidateRespond respond =
                candidateService.parseAndSaveCandidate(file, null);

        assertNotNull(respond);
        assertEquals("cand-123", respond.getCandidateId());
        assertEquals("Ahmed", respond.getFirstName());
        assertEquals("Ali", respond.getLastName());
        assertEquals("ahmed.ali@example.com", respond.getEmail());
        assertEquals("+201012345678", respond.getPhone());
        assertEquals(5, respond.getYearsOfExperience());

        // CV content is not retained
        assertNull(respond.getCvFilePath());

        assertTrue(respond.getSkills().contains("Java"));
        assertTrue(respond.getSkills().contains("Spring Boot"));
        assertTrue(respond.getSkills().contains("Docker"));

        verify(cvParsingService).parseInMemory(file);
        verify(candidateRepo).existsByEmail("ahmed.ali@example.com");
        verify(candidateRepo).save(any(CandidateEntity.class));
    }

    @Test
    @DisplayName("Should start bulk CV upload and return trackable job status")
    void testStartBulkCvUpload() {

        MockMultipartFile file1 = new MockMultipartFile(
                "files",
                "cv1.pdf",
                "application/pdf",
                "Content 1".getBytes()
        );

        MockMultipartFile file2 = new MockMultipartFile(
                "files",
                "cv2.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "Content 2".getBytes()
        );

        BulkUploadJob mockJob =
                new BulkUploadJob("job-abc-123", 2);

        when(bulkCvProcessingService.createJob(2))
                .thenReturn(mockJob);

        BulkUploadProgressRespond progress =
                candidateService.startBulkCvUpload(
                        List.of(file1, file2),
                        null
                );

        assertNotNull(progress);
        assertEquals("job-abc-123", progress.getJobId());
        assertEquals("PENDING", progress.getStatus());
        assertEquals(2, progress.getTotalFiles());

        verify(bulkCvProcessingService)
                .createJob(2);

        verify(bulkCvProcessingService)
                .processBulkUploadAsync(
                        eq("job-abc-123"),
                        anyList(),
                        isNull()
                );
    }

    @Test
    @DisplayName("Should reject bulk upload when exceeding max files limit")
    void testBulkUploadExceedsLimit() {

        List<org.springframework.web.multipart.MultipartFile> files =
                new java.util.ArrayList<>();

        for (int i = 0; i < 21; i++) {
            files.add(
                    new MockMultipartFile(
                            "files",
                            "cv" + i + ".pdf",
                            "application/pdf",
                            "content".getBytes()
                    )
            );
        }

        assertThrows(
                IllegalArgumentException.class,
                () -> candidateService.startBulkCvUpload(files, null)
        );
    }
}