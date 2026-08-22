package com.banquemisr.recruitment.candidate;

import com.banquemisr.recruitment.data.entity.*;
import com.banquemisr.recruitment.data.enums.ApplicationStatus;
import com.banquemisr.recruitment.data.enums.JobStatus;
import com.banquemisr.recruitment.data.enums.Role;
import com.banquemisr.recruitment.data.repo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CandidateSearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CandidateRepo candidateRepo;

    @Autowired
    private SkillRepo skillRepo;

    @Autowired
    private TagRepo tagRepo;

    @Autowired
    private JobRepo jobRepo;

    @Autowired
    private ApplicationRepo applicationRepo;

    @Autowired
    private UserRepo userRepo;

    @BeforeEach
    void setUp() {
        applicationRepo.deleteAll();
        jobRepo.deleteAll();
        candidateRepo.deleteAll();
        skillRepo.deleteAll();
        tagRepo.deleteAll();
        userRepo.deleteAll();

        UserEntity hrUser = userRepo.save(UserEntity.builder()
                .userEmail("recruiter@banquemisr.com")
                .userPassword("pass")
                .userFname("Recruiter")
                .userLname("User")
                .role(Role.ROLE_HR)
                .enabled(true)
                .build());

        SkillEntity javaSkill = skillRepo.save(SkillEntity.builder().name("Java").build());
        SkillEntity springSkill = skillRepo.save(SkillEntity.builder().name("Spring Boot").build());
        SkillEntity reactSkill = skillRepo.save(SkillEntity.builder().name("React").build());

        TagEntity seniorTag = tagRepo.save(TagEntity.builder().name("Senior").build());
        TagEntity immediateTag = tagRepo.save(TagEntity.builder().name("Immediate Joiner").build());

        CandidateEntity cand1 = candidateRepo.save(CandidateEntity.builder()
                .firstName("Ahmed")
                .lastName("Mohamed")
                .email("ahmed.mohamed@example.com")
                .phone("+201011111111")
                .yearsOfExperience(6)
                .skills(Set.of(javaSkill, springSkill))
                .tags(Set.of(seniorTag, immediateTag))
                .build());

        CandidateEntity cand2 = candidateRepo.save(CandidateEntity.builder()
                .firstName("Sara")
                .lastName("Ibrahim")
                .email("sara.ibrahim@example.com")
                .phone("+201022222222")
                .yearsOfExperience(3)
                .skills(Set.of(reactSkill))
                .tags(Set.of(immediateTag))
                .build());

        CandidateEntity cand3 = candidateRepo.save(CandidateEntity.builder()
                .firstName("Mahmoud")
                .lastName("Hassan")
                .email("mahmoud.hassan@example.com")
                .phone("+201033333333")
                .yearsOfExperience(8)
                .skills(Set.of(javaSkill))
                .tags(Set.of(seniorTag))
                .build());

        JobEntity backendJob = jobRepo.save(JobEntity.builder()
                .title("Senior Backend Engineer")
                .description("Java/Spring Boot developer")
                .department("Engineering")
                .location("Cairo")
                .status(JobStatus.OPEN)
                .createdBy(hrUser)
                .build());

        applicationRepo.save(ApplicationEntity.builder()
                .candidate(cand1)
                .job(backendJob)
                .status(ApplicationStatus.INTERVIEW)
                .assignedRecruiter(hrUser)
                .build());
    }

    @Test
    @WithMockUser(username = "interviewer@banquemisr.com", roles = {"INTERVIEWER"})
    @DisplayName("Search by name should return matching candidates")
    void testSearchByName() throws Exception {
        mockMvc.perform(get("/api/v1/candidates/search")
                        .param("name", "ahmed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].firstName").value("Ahmed"));
    }

    @Test
    @WithMockUser(username = "hr@banquemisr.com", roles = {"HR"})
    @DisplayName("Search by skill should return candidates having the requested skill")
    void testSearchBySkill() throws Exception {
        mockMvc.perform(get("/api/v1/candidates/search")
                        .param("skills", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].firstName", hasItem("Ahmed")))
                .andExpect(jsonPath("$.content[*].firstName", hasItem("Mahmoud")));
    }

    @Test
    @WithMockUser(username = "hr@banquemisr.com", roles = {"HR"})
    @DisplayName("Search by tag should return candidates having the tag")
    void testSearchByTag() throws Exception {
        mockMvc.perform(get("/api/v1/candidates/search")
                        .param("tags", "Immediate Joiner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].firstName", hasItem("Ahmed")))
                .andExpect(jsonPath("$.content[*].firstName", hasItem("Sara")));
    }

    @Test
    @WithMockUser(username = "hr@banquemisr.com", roles = {"HR"})
    @DisplayName("Search by application status should find candidates with active application status")
    void testSearchByApplicationStatus() throws Exception {
        mockMvc.perform(get("/api/v1/candidates/search")
                        .param("status", "INTERVIEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].email").value("ahmed.mohamed@example.com"));
    }

    @Test
    @WithMockUser(username = "admin@banquemisr.com", roles = {"ADMIN"})
    @DisplayName("Search with pagination and sorting by years of experience descending")
    void testSearchPaginationAndSorting() throws Exception {
        mockMvc.perform(get("/api/v1/candidates/search")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "yearsOfExperience,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].yearsOfExperience").value(8))
                .andExpect(jsonPath("$.content[1].yearsOfExperience").value(6));
    }

    @Test
    @WithMockUser(username = "hr@banquemisr.com", roles = {"HR"})
    @DisplayName("Combined search: skill + minExperience")
    void testCombinedFilterSearch() throws Exception {
        mockMvc.perform(get("/api/v1/candidates/search")
                        .param("skills", "Java")
                        .param("minExperience", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].firstName").value("Mahmoud"));
    }
}
