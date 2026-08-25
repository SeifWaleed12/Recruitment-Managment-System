package com.banquemisr.recruitment.data.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkillEntityTest {

    @Test
    void builder_withoutCandidates_initializesEmptySetNotNull() {
        SkillEntity skill = SkillEntity.builder()
                .name("Java")
                .build();

        assertThat(skill.getCandidates()).isNotNull().isEmpty();
    }

    @Test
    void builder_setsAllFieldsCorrectly() {
        SkillEntity skill = SkillEntity.builder()
                .id("skill-1")
                .name("Java")
                .build();

        assertThat(skill.getId()).isEqualTo("skill-1");
        assertThat(skill.getName()).isEqualTo("Java");
    }

    @Test
    void getSkillId_returnsSameValueAsGetId() {
        SkillEntity skill = SkillEntity.builder()
                .id("skill-1")
                .name("Java")
                .build();

        assertThat(skill.getSkillId()).isEqualTo("skill-1");
    }

    @Test
    void setSkillId_updatesUnderlyingIdField() {
        SkillEntity skill = SkillEntity.builder()
                .name("Java")
                .build();

        skill.setSkillId("skill-2");

        assertThat(skill.getId()).isEqualTo("skill-2");
        assertThat(skill.getSkillId()).isEqualTo("skill-2");
    }

    @Test
    void addingCandidate_appearsInCandidatesSet() {
        SkillEntity skill = SkillEntity.builder().name("Java").build();
        CandidateEntity candidate = CandidateEntity.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane@example.com")
                .build();

        skill.getCandidates().add(candidate);

        assertThat(skill.getCandidates()).hasSize(1).contains(candidate);
    }
}