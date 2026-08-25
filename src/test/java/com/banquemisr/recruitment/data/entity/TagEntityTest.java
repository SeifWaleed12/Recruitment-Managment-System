package com.banquemisr.recruitment.data.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TagEntityTest {

    @Test
    void builder_withoutCandidates_initializesEmptySetNotNull() {
        TagEntity tag = TagEntity.builder()
                .name("Urgent")
                .build();

        assertThat(tag.getCandidates()).isNotNull().isEmpty();
    }

    @Test
    void builder_setsAllFieldsCorrectly() {
        TagEntity tag = TagEntity.builder()
                .tagId("tag-1")
                .name("Urgent")
                .build();

        assertThat(tag.getTagId()).isEqualTo("tag-1");
        assertThat(tag.getName()).isEqualTo("Urgent");
    }

    @Test
    void setName_updatesNameFreely() {
        TagEntity tag = TagEntity.builder()
                .tagId("tag-1")
                .name("Urgent")
                .build();

        tag.setName("Priority");

        assertThat(tag.getName()).isEqualTo("Priority");
    }

    @Test
    void addingCandidate_appearsInCandidatesSet() {
        TagEntity tag = TagEntity.builder().name("Urgent").build();
        CandidateEntity candidate = CandidateEntity.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane@example.com")
                .build();

        tag.getCandidates().add(candidate);

        assertThat(tag.getCandidates()).hasSize(1).contains(candidate);
    }

    @Test
    void removingCandidate_noLongerAppearsInCandidatesSet() {
        TagEntity tag = TagEntity.builder().name("Urgent").build();
        CandidateEntity candidate = CandidateEntity.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane@example.com")
                .build();
        tag.getCandidates().add(candidate);

        tag.getCandidates().remove(candidate);

        assertThat(tag.getCandidates()).isEmpty();
    }
}