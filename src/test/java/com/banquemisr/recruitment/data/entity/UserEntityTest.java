package com.banquemisr.recruitment.data.entity;

import com.banquemisr.recruitment.data.enums.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserEntityTest {

    @Test
    void builder_withoutEnabled_defaultsToTrue() {
        UserEntity user = UserEntity.builder()
                .userEmail("jane@example.com")
                .userFname("Jane")
                .userLname("Doe")
                .role(Role.ROLE_HR)
                .build();

        assertThat(user.getEnabled()).isTrue();
    }

    @Test
    void builder_setsAllFieldsCorrectly() {
        UserEntity user = UserEntity.builder()
                .userId("user-1")
                .userEmail("jane@example.com")
                .userFname("Jane")
                .userLname("Doe")
                .role(Role.ROLE_ADMIN)
                .enabled(false)
                .build();

        assertThat(user.getUserId()).isEqualTo("user-1");
        assertThat(user.getUserEmail()).isEqualTo("jane@example.com");
        assertThat(user.getUserFname()).isEqualTo("Jane");
        assertThat(user.getUserLname()).isEqualTo("Doe");
        assertThat(user.getRole()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(user.getEnabled()).isFalse();
    }

    @Test
    void setEnabled_togglesFlag() {
        UserEntity user = UserEntity.builder()
                .userEmail("jane@example.com")
                .userFname("Jane")
                .userLname("Doe")
                .role(Role.ROLE_HR)
                .build();

        user.setEnabled(false);

        assertThat(user.getEnabled()).isFalse();
    }

    @Test
    void setRole_changesRoleFreely_noRestrictionCurrentlyEnforced() {
        UserEntity user = UserEntity.builder()
                .userEmail("jane@example.com")
                .userFname("Jane")
                .userLname("Doe")
                .role(Role.ROLE_INTERVIEWER)
                .build();

        user.setRole(Role.ROLE_ADMIN);

        assertThat(user.getRole()).isEqualTo(Role.ROLE_ADMIN);
    }
}