package com.banquemisr.recruitment.data.repo;

import com.banquemisr.recruitment.data.entity.TagEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TagRepoTest {

    @Autowired
    private TagRepo tagRepo;

    @Autowired
    private EntityManager entityManager;

    private TagEntity persistTag(String name) {
        TagEntity tag = TagEntity.builder().name(name).build();
        entityManager.persist(tag);
        entityManager.flush();
        return tag;
    }

    @Test
    void findByNameIgnoreCase_whenExists_returnsTagRegardlessOfCase() {
        String name = "Urgent-" + UUID.randomUUID();
        TagEntity saved = persistTag(name);

        Optional<TagEntity> result = tagRepo.findByNameIgnoreCase(name.toUpperCase());

        assertThat(result).isPresent();
        assertThat(result.get().getTagId()).isEqualTo(saved.getTagId());
    }

    @Test
    void findByNameIgnoreCase_whenNotExists_returnsEmpty() {
        Optional<TagEntity> result = tagRepo.findByNameIgnoreCase("NonExistentTag");

        assertThat(result).isEmpty();
    }

    @Test
    void existsById_whenTagPersisted_returnsTrue() {
        TagEntity saved = persistTag("Priority-" + UUID.randomUUID());

        assertThat(tagRepo.existsById(saved.getTagId())).isTrue();
    }

    @Test
    void deleteById_removesTagFromRepository() {
        TagEntity saved = persistTag("Temp-" + UUID.randomUUID());

        tagRepo.deleteById(saved.getTagId());
        entityManager.flush();

        assertThat(tagRepo.existsById(saved.getTagId())).isFalse();
    }
}