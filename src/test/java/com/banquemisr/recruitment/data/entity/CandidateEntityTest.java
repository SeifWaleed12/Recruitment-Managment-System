package com.banquemisr.recruitment.data.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CandidateEntityTest {

    @Test
    void builder_withoutYearsOfExperience_defaultsToZero() {
        CandidateEntity candidate = CandidateEntity.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane@example.com")
                .build();

        assertThat(candidate.getYearsOfExperience()).isEqualTo(0);
    }

    @Test
    void builder_withoutSkillsOrTags_initializesEmptySetsNotNull() {
        CandidateEntity candidate = CandidateEntity.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane@example.com")
                .build();

        assertThat(candidate.getSkills()).isNotNull().isEmpty();
        assertThat(candidate.getTags()).isNotNull().isEmpty();
    }

    @Test
    void addingSkill_appearsInSkillsSet() {
        CandidateEntity candidate = CandidateEntity.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane@example.com")
                .build();
        SkillEntity java = SkillEntity.builder().name("Java").build();

        candidate.getSkills().add(java);

        assertThat(candidate.getSkills()).hasSize(1).contains(java);
    }

    @Test
    void addingSameSkillTwice_doesNotDuplicate() {
        CandidateEntity candidate = CandidateEntity.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane@example.com")
                .build();
        SkillEntity java = SkillEntity.builder().id("skill-1").name("Java").build();

        candidate.getSkills().add(java);
        candidate.getSkills().add(java);

        assertThat(candidate.getSkills()).hasSize(1);
    }

    @Test
    void removingTag_noLongerAppearsInTagsSet() {
        CandidateEntity candidate = CandidateEntity.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane@example.com")
                .build();
        TagEntity urgent = TagEntity.builder().name("Urgent").build();
        candidate.getTags().add(urgent);

        candidate.getTags().remove(urgent);

        assertThat(candidate.getTags()).isEmpty();
    }
}