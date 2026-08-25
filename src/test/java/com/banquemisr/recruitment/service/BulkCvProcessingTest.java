package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.cvparsing.extractor.CvDataExtractor;
import com.banquemisr.recruitment.cvparsing.model.BulkUploadJob;
import com.banquemisr.recruitment.cvparsing.model.ParsedCv;
import com.banquemisr.recruitment.cvparsing.parser.CvParserRegistry;
import com.banquemisr.recruitment.cvparsing.service.BulkCvProcessingService;
import com.banquemisr.recruitment.data.entity.CandidateEntity;
import com.banquemisr.recruitment.data.entity.SkillEntity;
import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.repo.CandidateRepo;
import com.banquemisr.recruitment.data.repo.SkillRepo;
import com.banquemisr.recruitment.data.repo.UserRepo;
import com.banquemisr.recruitment.exception.ResourceNotFoundException;
import com.banquemisr.recruitment.mapper.CandidateMapper;
import com.banquemisr.recruitment.web.DTOs.respond.CandidateRespond;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BulkCvProcessingServiceTest {

    @Mock
    private CvParserRegistry parserRegistry;

    @Mock
    private CvDataExtractor dataExtractor;

    @Mock
    private CandidateRepo candidateRepo;

    @Mock
    private SkillRepo skillRepo;

    @Mock
    private UserRepo userRepo;

    @Mock
    private CandidateMapper candidateMapper;

    @InjectMocks
    private BulkCvProcessingService bulkCvProcessingService;

    // ---------- createJob / getJob ----------

    @Test
    void createJob_registersAndReturnsPendingJob() {

        BulkUploadJob job = bulkCvProcessingService.createJob(3);

        assertThat(job.getTotalFiles()).isEqualTo(3);
        assertThat(job.getStatus()).isEqualTo(BulkUploadJob.JobStatus.PENDING);
        assertThat(bulkCvProcessingService.getJob(job.getJobId())).isEqualTo(job);
    }

    @Test
    void getJob_whenNotFound_throwsResourceNotFoundException() {

        assertThatThrownBy(() -> bulkCvProcessingService.getJob("missing-job"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- processSingleInMemoryFile ----------

    @Test
    void processSingleInMemoryFile_whenBytesEmpty_throwsIllegalArgumentException() {

        BulkCvProcessingService.InMemoryFile file =
                new BulkCvProcessingService.InMemoryFile("cv.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> bulkCvProcessingService.processSingleInMemoryFile(file, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void processSingleInMemoryFile_whenBytesNull_throwsIllegalArgumentException() {

        BulkCvProcessingService.InMemoryFile file =
                new BulkCvProcessingService.InMemoryFile("cv.pdf", "application/pdf", null);

        assertThatThrownBy(() -> bulkCvProcessingService.processSingleInMemoryFile(file, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void processSingleInMemoryFile_whenEmailUnique_savesCandidateWithResolvedSkills() throws Exception {

        BulkCvProcessingService.InMemoryFile file =
                new BulkCvProcessingService.InMemoryFile("cv.pdf", "application/pdf", "cv bytes".getBytes());
        ParsedCv parsedCv = ParsedCv.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane@example.com")
                .skills(Set.of("Java"))
                .build();
        SkillEntity skill = SkillEntity.builder().id("skill-1").name("Java").build();
        CandidateEntity savedEntity = CandidateEntity.builder().candidateId("cand-1").email("jane@example.com").build();
        CandidateRespond respond = CandidateRespond.builder().candidateId("cand-1").build();

        when(parserRegistry.parseToText(any(InputStream.class), eq("application/pdf"), eq("cv.pdf")))
                .thenReturn("raw text");
        when(dataExtractor.extract("raw text", "cv.pdf")).thenReturn(parsedCv);
        when(candidateRepo.existsByEmail("jane@example.com")).thenReturn(false);
        when(skillRepo.findByNameIgnoreCase("Java")).thenReturn(Optional.of(skill));
        when(candidateRepo.save(any(CandidateEntity.class))).thenReturn(savedEntity);
        when(candidateMapper.toRespond(savedEntity)).thenReturn(respond);

        CandidateRespond result = bulkCvProcessingService.processSingleInMemoryFile(file, null);

        assertThat(result.getCandidateId()).isEqualTo("cand-1");
        verify(candidateRepo).save(argThat(entity ->
                entity.getEmail().equals("jane@example.com")
                        && entity.getSkills().contains(skill)
                        && entity.getCvFilePath() == null
        ));
    }

    @Test
    void processSingleInMemoryFile_whenEmailAlreadyExists_resolvesUniqueEmail() throws Exception {

        BulkCvProcessingService.InMemoryFile file =
                new BulkCvProcessingService.InMemoryFile("cv.pdf", "application/pdf", "cv bytes".getBytes());
        ParsedCv parsedCv = ParsedCv.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane@example.com")
                .skills(Set.of())
                .build();
        CandidateEntity savedEntity = CandidateEntity.builder().candidateId("cand-1").build();

        when(parserRegistry.parseToText(any(InputStream.class), eq("application/pdf"), eq("cv.pdf")))
                .thenReturn("raw text");
        when(dataExtractor.extract("raw text", "cv.pdf")).thenReturn(parsedCv);
        when(candidateRepo.existsByEmail("jane@example.com")).thenReturn(true);
        when(candidateRepo.save(any(CandidateEntity.class))).thenReturn(savedEntity);
        when(candidateMapper.toRespond(savedEntity)).thenReturn(CandidateRespond.builder().candidateId("cand-1").build());

        bulkCvProcessingService.processSingleInMemoryFile(file, null);

        verify(candidateRepo).save(argThat(entity ->
                !entity.getEmail().equals("jane@example.com") && entity.getEmail().contains("jane+")
        ));
    }

    @Test
    void processSingleInMemoryFile_setsCreator_whenProvided() throws Exception {

        BulkCvProcessingService.InMemoryFile file =
                new BulkCvProcessingService.InMemoryFile("cv.pdf", "application/pdf", "cv bytes".getBytes());
        ParsedCv parsedCv = ParsedCv.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane@example.com")
                .skills(Set.of())
                .build();
        UserEntity creator = UserEntity.builder().userId("user-1").build();
        CandidateEntity savedEntity = CandidateEntity.builder().candidateId("cand-1").build();

        when(parserRegistry.parseToText(any(InputStream.class), eq("application/pdf"), eq("cv.pdf")))
                .thenReturn("raw text");
        when(dataExtractor.extract("raw text", "cv.pdf")).thenReturn(parsedCv);
        when(candidateRepo.existsByEmail("jane@example.com")).thenReturn(false);
        when(candidateRepo.save(any(CandidateEntity.class))).thenReturn(savedEntity);
        when(candidateMapper.toRespond(savedEntity)).thenReturn(CandidateRespond.builder().candidateId("cand-1").build());

        bulkCvProcessingService.processSingleInMemoryFile(file, creator);

        verify(candidateRepo).save(argThat(entity -> entity.getCreatedBy() == creator));
    }

    // ---------- processBulkUploadAsync ----------

    @Test
    void processBulkUploadAsync_whenAllFilesSucceed_marksJobCompleted() throws Exception {

        BulkUploadJob job = bulkCvProcessingService.createJob(1);
        BulkCvProcessingService.InMemoryFile file =
                new BulkCvProcessingService.InMemoryFile("cv.pdf", "application/pdf", "cv bytes".getBytes());
        ParsedCv parsedCv = ParsedCv.builder()
                .firstName("Jane").lastName("Doe").email("jane@example.com").skills(Set.of()).build();
        CandidateEntity savedEntity = CandidateEntity.builder().candidateId("cand-1").build();

        when(userRepo.findById("user-1")).thenReturn(Optional.of(UserEntity.builder().userId("user-1").build()));
        when(parserRegistry.parseToText(any(InputStream.class), eq("application/pdf"), eq("cv.pdf")))
                .thenReturn("raw text");
        when(dataExtractor.extract("raw text", "cv.pdf")).thenReturn(parsedCv);
        when(candidateRepo.existsByEmail("jane@example.com")).thenReturn(false);
        when(candidateRepo.save(any(CandidateEntity.class))).thenReturn(savedEntity);
        when(candidateMapper.toRespond(savedEntity)).thenReturn(CandidateRespond.builder().candidateId("cand-1").build());

        bulkCvProcessingService.processBulkUploadAsync(job.getJobId(), List.of(file), "user-1");

        assertThat(job.getStatus()).isEqualTo(BulkUploadJob.JobStatus.COMPLETED);
        assertThat(job.getSuccessfulFiles().get()).isEqualTo(1);
        assertThat(job.getFailedFiles().get()).isEqualTo(0);
    }

    @Test
    void processBulkUploadAsync_whenAllFilesFail_marksJobFailedAndRecordsErrors() throws Exception {

        BulkUploadJob job = bulkCvProcessingService.createJob(1);
        BulkCvProcessingService.InMemoryFile file =
                new BulkCvProcessingService.InMemoryFile("cv.pdf", "application/pdf", "cv bytes".getBytes());

        when(parserRegistry.parseToText(any(InputStream.class), eq("application/pdf"), eq("cv.pdf")))
                .thenThrow(new RuntimeException("parsing failed"));

        bulkCvProcessingService.processBulkUploadAsync(job.getJobId(), List.of(file), null);

        assertThat(job.getStatus()).isEqualTo(BulkUploadJob.JobStatus.FAILED);
        assertThat(job.getFailedFiles().get()).isEqualTo(1);
        assertThat(job.getErrors()).hasSize(1);
        verifyNoInteractions(userRepo);
    }

    @Test
    void processBulkUploadAsync_whenCreatorNotFound_continuesWithNullCreator() throws Exception {

        BulkUploadJob job = bulkCvProcessingService.createJob(1);
        BulkCvProcessingService.InMemoryFile file =
                new BulkCvProcessingService.InMemoryFile("cv.pdf", "application/pdf", "cv bytes".getBytes());
        ParsedCv parsedCv = ParsedCv.builder()
                .firstName("Jane").lastName("Doe").email("jane@example.com").skills(Set.of()).build();
        CandidateEntity savedEntity = CandidateEntity.builder().candidateId("cand-1").build();

        when(userRepo.findById("missing-user")).thenReturn(Optional.empty());
        when(parserRegistry.parseToText(any(InputStream.class), eq("application/pdf"), eq("cv.pdf")))
                .thenReturn("raw text");
        when(dataExtractor.extract("raw text", "cv.pdf")).thenReturn(parsedCv);
        when(candidateRepo.existsByEmail("jane@example.com")).thenReturn(false);
        when(candidateRepo.save(any(CandidateEntity.class))).thenReturn(savedEntity);
        when(candidateMapper.toRespond(savedEntity)).thenReturn(CandidateRespond.builder().candidateId("cand-1").build());

        bulkCvProcessingService.processBulkUploadAsync(job.getJobId(), List.of(file), "missing-user");

        assertThat(job.getStatus()).isEqualTo(BulkUploadJob.JobStatus.COMPLETED);
        verify(candidateRepo).save(argThat(entity -> entity.getCreatedBy() == null));
    }
}