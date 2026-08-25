package com.banquemisr.recruitment.data.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenEntityTest {

    @Test
    void builder_setsAllFieldsCorrectly() {
        UserEntity user = UserEntity.builder().userId("user-1").build();
        Instant expiresAt = Instant.now().plusSeconds(86400);

        RefreshTokenEntity token = RefreshTokenEntity.builder()
                .id("token-1")
                .user(user)
                .tokenHash("hashed-value")
                .expiresAt(expiresAt)
                .build();

        assertThat(token.getId()).isEqualTo("token-1");
        assertThat(token.getUser()).isSameAs(user);
        assertThat(token.getTokenHash()).isEqualTo("hashed-value");
        assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void builder_withoutCreationTimestamp_leavesCreatedAtNullUntilPersisted() {
        RefreshTokenEntity token = RefreshTokenEntity.builder()
                .user(UserEntity.builder().userId("user-1").build())
                .tokenHash("hashed-value")
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        assertThat(token.getCreatedAt()).isNull();
    }

    @Test
    void setTokenHash_rotatesStoredHash() {
        RefreshTokenEntity token = RefreshTokenEntity.builder()
                .user(UserEntity.builder().userId("user-1").build())
                .tokenHash("old-hash")
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        token.setTokenHash("new-hash");

        assertThat(token.getTokenHash()).isEqualTo("new-hash");
    }

    @Test
    void expiresAt_beforeNow_isConsideredExpired() {
        Instant past = Instant.now().minusSeconds(60);
        RefreshTokenEntity token = RefreshTokenEntity.builder()
                .user(UserEntity.builder().userId("user-1").build())
                .tokenHash("hash")
                .expiresAt(past)
                .build();

        assertThat(token.getExpiresAt().isBefore(Instant.now())).isTrue();
    }
}