package com.banquemisr.recruitment.data.repo;

import com.banquemisr.recruitment.data.entity.CandidateEntity;
import com.banquemisr.recruitment.data.entity.SkillEntity;
import com.banquemisr.recruitment.data.entity.TagEntity;
import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.enums.Role;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CandidateRepoTest {

    @Autowired
    private CandidateRepo candidateRepo;

    @Autowired
    private EntityManager entityManager;

    private CandidateEntity persistCandidateWithSkillsAndTags() {
        UserEntity recruiter = UserEntity.builder()
                .userEmail("hr-" + UUID.randomUUID() + "@example.com")
                .userFname("Sara")
                .userLname("Ahmed")
                .role(Role.ROLE_HR)
                .build();
        entityManager.persist(recruiter);

        SkillEntity java = SkillEntity.builder().name("Java-" + UUID.randomUUID()).build();
        TagEntity urgent = TagEntity.builder().name("Urgent-" + UUID.randomUUID()).build();
        entityManager.persist(java);
        entityManager.persist(urgent);

        Set<SkillEntity> skills = new HashSet<>();
        skills.add(java);
        Set<TagEntity> tags = new HashSet<>();
        tags.add(urgent);

        CandidateEntity candidate = CandidateEntity.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane-" + UUID.randomUUID() + "@example.com")
                .cvFilePath("/cv/jane.pdf")
                .cvOriginalFilename("jane.pdf")
                .createdBy(recruiter)
                .skills(skills)
                .tags(tags)
                .build();
        entityManager.persist(candidate);

        entityManager.flush();
        entityManager.clear();
        return candidate;
    }

    @Test
    void findByEmail_whenExists_returnsCandidate() {
        CandidateEntity saved = persistCandidateWithSkillsAndTags();

        Optional<CandidateEntity> result = candidateRepo.findByEmail(saved.getEmail());

        assertThat(result).isPresent();
        assertThat(result.get().getCandidateId()).isEqualTo(saved.getCandidateId());
    }

    @Test
    void findByEmail_whenNotExists_returnsEmpty() {
        Optional<CandidateEntity> result = candidateRepo.findByEmail("nobody@example.com");

        assertThat(result).isEmpty();
    }

    @Test
    void existsByEmail_whenExists_returnsTrue() {
        CandidateEntity saved = persistCandidateWithSkillsAndTags();

        assertThat(candidateRepo.existsByEmail(saved.getEmail())).isTrue();
    }

    @Test
    void existsByEmail_whenNotExists_returnsFalse() {
        assertThat(candidateRepo.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    void findById_eagerlyLoadsSkillsTagsAndCreatedBy() {
        CandidateEntity saved = persistCandidateWithSkillsAndTags();

        Optional<CandidateEntity> result = candidateRepo.findById(saved.getCandidateId());

        assertThat(result).isPresent();
        CandidateEntity found = result.get();
        assertThat(found.getSkills()).hasSize(1);
        assertThat(found.getTags()).hasSize(1);
        assertThat(found.getCreatedBy().getUserFname()).isEqualTo("Sara");
    }

    @Test
    void findAllWithSkillsAndTags_returnsDistinctCandidatesWithRelationsLoaded() {
        persistCandidateWithSkillsAndTags();

        List<CandidateEntity> results = candidateRepo.findAllWithSkillsAndTags();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSkills()).isNotEmpty();
    }

    @Test
    void findAllPageable_returnsPagedResults() {
        persistCandidateWithSkillsAndTags();
        Pageable pageable = PageRequest.of(0, 10);

        var page = candidateRepo.findAll(pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getSkills()).isNotEmpty();
    }
}