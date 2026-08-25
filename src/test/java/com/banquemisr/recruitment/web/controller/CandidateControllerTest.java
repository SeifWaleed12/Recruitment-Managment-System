package com.banquemisr.recruitment.web.controller;

import com.banquemisr.recruitment.cvparsing.model.BulkUploadProgressRespond;
import com.banquemisr.recruitment.data.enums.ApplicationStatus;
import com.banquemisr.recruitment.service.CandidateService;
import com.banquemisr.recruitment.web.DTOs.respond.CandidateRespond;
import com.banquemisr.recruitment.web.DTOs.respond.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import com.banquemisr.recruitment.Authentication.Security.JwtService;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CandidateController.class)
class CandidateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CandidateService candidateService;
    @MockBean
    private JwtService jwtService;


    // ---------- GET /api/v1/candidates — ADMIN, HR, INTERVIEWER ----------

    @Test
    @WithMockUser(roles = "HR")
    void getAllCandidates_asHr_returns200() throws Exception {

        PageResponse<CandidateRespond> response = new PageResponse<>();

        when(candidateService.getAllCandidates(any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/candidates"))
                .andExpect(status().isOk());
    }


    @Test
    @WithMockUser(roles = "INTERVIEWER")
    void getAllCandidates_asInterviewer_returns200() throws Exception {

        when(candidateService.getAllCandidates(any()))
                .thenReturn(new PageResponse<>());

        mockMvc.perform(get("/api/v1/candidates"))
                .andExpect(status().isOk());
    }


    @Test
    void getAllCandidates_whenNotAuthenticated_returns401() throws Exception {

        mockMvc.perform(get("/api/v1/candidates"))
                .andExpect(status().isUnauthorized());
    }


    // ---------- GET /{id} ----------

    @Test
    @WithMockUser(roles = "HR")
    void getCandidateById_returns200() throws Exception {

        CandidateRespond response = CandidateRespond.builder()
                .candidateId("cand-1")
                .build();

        when(candidateService.getCandidateById("cand-1"))
                .thenReturn(response);


        mockMvc.perform(get("/api/v1/candidates/cand-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateId").value("cand-1"));
    }


    @Test
    @WithMockUser(roles = "INTERVIEWER")
    void getCandidateById_asInterviewer_returns200() throws Exception {

        when(candidateService.getCandidateById("cand-1"))
                .thenReturn(CandidateRespond.builder()
                        .candidateId("cand-1")
                        .build());


        mockMvc.perform(get("/api/v1/candidates/cand-1"))
                .andExpect(status().isOk());
    }



    // ---------- GET /search ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void searchCandidates_returns200() throws Exception {

        when(candidateService.searchCandidates(any(), any()))
                .thenReturn(new PageResponse<>());


        mockMvc.perform(get("/api/v1/candidates/search")
                        .param("name", "John")
                        .param("minExperience", "2"))
                .andExpect(status().isOk());
    }



    // ---------- POST /upload ----------

    @Test
    @WithMockUser(roles = "HR")
    void uploadCv_asHr_returns201() throws Exception {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "cv.pdf",
                        MediaType.APPLICATION_PDF_VALUE,
                        "test pdf".getBytes()
                );


        CandidateRespond response = CandidateRespond.builder()
                .candidateId("cand-1")
                .build();


        when(candidateService.parseAndSaveCandidate(any(), anyString()))
                .thenReturn(response);


        mockMvc.perform(
                        multipart("/api/v1/candidates/upload")
                                .file(file)
                                .param("createdByUserId", "user-1")
                                .with(csrf())
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.candidateId").value("cand-1"));
    }



    @Test
    @WithMockUser(roles = "INTERVIEWER")
    void uploadCv_asInterviewer_returns403() throws Exception {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "cv.pdf",
                        MediaType.APPLICATION_PDF_VALUE,
                        "test".getBytes()
                );


        mockMvc.perform(multipart("/api/v1/candidates/upload")
                        .file(file)
                        .param("createdByUserId", "user-1"))
                .andExpect(status().isForbidden());
    }



    // ---------- POST /bulk-upload ----------

    @Test
    @WithMockUser(roles = "HR")
    void bulkUpload_asHr_returns202() throws Exception {


        BulkUploadProgressRespond response =
                BulkUploadProgressRespond.builder()
                        .jobId("job-1")
                        .build();


        when(candidateService.startBulkCvUpload(anyList(), any()))
                .thenReturn(response);


        MockMultipartFile file =
                new MockMultipartFile(
                        "files",
                        "cv.pdf",
                        MediaType.APPLICATION_PDF_VALUE,
                        "test".getBytes()
                );


        mockMvc.perform(
                        multipart("/api/v1/candidates/bulk-upload")
                                .file(file)
                                .param("createdByUserId", "user-1")
                                .with(csrf())
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value("job-1"));
    }



    // ---------- GET /bulk-upload/{jobId}/status ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void getBulkUploadStatus_returns200() throws Exception {

        BulkUploadProgressRespond response =
                BulkUploadProgressRespond.builder()
                        .jobId("job-1")
                        .build();


        when(candidateService.getBulkUploadProgress("job-1"))
                .thenReturn(response);


        mockMvc.perform(get("/api/v1/candidates/bulk-upload/job-1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("job-1"));
    }



    // ---------- POST / ----------

    @Test
    @WithMockUser(roles = "HR")
    void createCandidate_asHr_returns201() throws Exception {


        CandidateRespond response =
                CandidateRespond.builder()
                        .candidateId("cand-1")
                        .build();


        when(candidateService.createCandidate(any()))
                .thenReturn(response);



        String requestBody = """
                {
                    "firstName":"John",
                    "lastName":"Doe",
                    "email":"john@test.com"
                }
                """;


        mockMvc.perform(post("/api/v1/candidates")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.candidateId").value("cand-1"));
    }



    @Test
    @WithMockUser(roles = "INTERVIEWER")
    void createCandidate_asInterviewer_returns403() throws Exception {


        mockMvc.perform(post("/api/v1/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }



    // ---------- PUT /{id} ----------

    @Test
    @WithMockUser(roles = "HR")
    void updateCandidate_returns200() throws Exception {


        when(candidateService.updateCandidate(anyString(), any()))
                .thenReturn(
                        CandidateRespond.builder()
                                .candidateId("cand-1")
                                .build()
                );


        mockMvc.perform(put("/api/v1/candidates/cand-1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "firstName":"Updated",
                              "lastName":"Doe",
                              "email":"updated@test.com"
                            }
                            """))
                .andExpect(status().isOk());
    }



    // ---------- DELETE /{id} ----------

    @Test
    @WithMockUser(roles = "HR")
    void deleteCandidate_returns204() throws Exception {

        mockMvc.perform(delete("/api/v1/candidates/cand-1")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }



    @Test
    @WithMockUser(roles = "INTERVIEWER")
    void deleteCandidate_asInterviewer_returns403() throws Exception {

        mockMvc.perform(delete("/api/v1/candidates/cand-1"))
                .andExpect(status().isForbidden());
    }



    // ---------- Skills ----------

    @Test
    @WithMockUser(roles = "HR")
    void assignSkill_returns200() throws Exception {


        when(candidateService.assignSkill("cand-1", "skill-1"))
                .thenReturn(
                        CandidateRespond.builder()
                                .candidateId("cand-1")
                                .build()
                );


        mockMvc.perform(post("/api/v1/candidates/cand-1/tags/tag-1")
                        .with(csrf()))
                .andExpect(status().isOk());
    }



    @Test
    @WithMockUser(roles = "HR")
    void removeSkill_returns200() throws Exception {


        when(candidateService.removeSkill("cand-1", "skill-1"))
                .thenReturn(
                        CandidateRespond.builder()
                                .candidateId("cand-1")
                                .build()
                );


        mockMvc.perform(delete("/api/v1/candidates/cand-1/tags/tag-1")
                        .with(csrf()))
                .andExpect(status().isOk());
    }



    // ---------- Tags ----------

    @Test
    @WithMockUser(roles = "HR")
    void assignTag_returns200() throws Exception {

        when(candidateService.assignTag("cand-1", "tag-1"))
                .thenReturn(
                        CandidateRespond.builder()
                                .candidateId("cand-1")
                                .build()
                );


        mockMvc.perform(post("/api/v1/candidates/cand-1/tags/tag-1")
                        .with(csrf()))
                .andExpect(status().isOk());
    }



    @Test
    @WithMockUser(roles = "HR")
    void removeTag_returns200() throws Exception {

        when(candidateService.removeTag("cand-1", "tag-1"))
                .thenReturn(
                        CandidateRespond.builder()
                                .candidateId("cand-1")
                                .build()
                );


        mockMvc.perform(delete("/api/v1/candidates/cand-1/tags/tag-1")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

}