package com.banquemisr.recruitment.data.repo;

import com.banquemisr.recruitment.data.entity.SkillEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class SkillRepoTest {

    @Autowired
    private SkillRepo skillRepo;

    @Autowired
    private EntityManager entityManager;

    private SkillEntity persistSkill(String name) {
        SkillEntity skill = SkillEntity.builder().name(name).build();
        entityManager.persist(skill);
        entityManager.flush();
        return skill;
    }

    @Test
    void findByNameIgnoreCase_whenExists_returnsSkillRegardlessOfCase() {
        String name = "Java-" + UUID.randomUUID();
        SkillEntity saved = persistSkill(name);

        Optional<SkillEntity> result = skillRepo.findByNameIgnoreCase(name.toUpperCase());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void findByNameIgnoreCase_whenNotExists_returnsEmpty() {
        Optional<SkillEntity> result = skillRepo.findByNameIgnoreCase("NonExistentSkill");

        assertThat(result).isEmpty();
    }

    @Test
    void findByNameIn_returnsOnlyMatchingSkills() {
        String java = "Java-" + UUID.randomUUID();
        String python = "Python-" + UUID.randomUUID();
        persistSkill(java);
        persistSkill(python);
        persistSkill("SQL-" + UUID.randomUUID());

        List<SkillEntity> result = skillRepo.findByNameIn(List.of(java, python));

        assertThat(result).hasSize(2)
                .extracting(SkillEntity::getName)
                .containsExactlyInAnyOrder(java, python);
    }

    @Test
    void findByNameIn_whenNoneMatch_returnsEmptyList() {
        persistSkill("Java-" + UUID.randomUUID());

        List<SkillEntity> result = skillRepo.findByNameIn(List.of("NonExistent"));

        assertThat(result).isEmpty();
    }
}