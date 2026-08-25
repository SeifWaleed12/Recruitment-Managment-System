package com.banquemisr.recruitment.data.repo;

import com.banquemisr.recruitment.data.entity.RefreshTokenEntity;
import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.enums.Role;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RefreshTokenRepoTest {

    @Autowired
    private RefreshTokenRepo refreshTokenRepo;

    @Autowired
    private EntityManager entityManager;

    private UserEntity persistUser() {
        UserEntity user = UserEntity.builder()
                .userEmail("user-" + UUID.randomUUID() + "@example.com")
                .userFname("Sara")
                .userLname("Ahmed")
                .role(Role.ROLE_HR)
                .build();
        entityManager.persist(user);
        return user;
    }

    private RefreshTokenEntity persistToken(UserEntity user, String tokenHash, Instant expiresAt) {
        RefreshTokenEntity token = RefreshTokenEntity.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .build();
        entityManager.persist(token);
        entityManager.flush();
        return token;
    }

    @Test
    void findByTokenHash_whenExists_returnsToken() {
        UserEntity user = persistUser();
        String hash = "hash-" + UUID.randomUUID();
        RefreshTokenEntity saved = persistToken(user, hash, Instant.now().plusSeconds(3600));

        Optional<RefreshTokenEntity> result = refreshTokenRepo.findByTokenHash(hash);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void findByTokenHash_whenNotExists_returnsEmpty() {
        Optional<RefreshTokenEntity> result = refreshTokenRepo.findByTokenHash("nonexistent-hash");

        assertThat(result).isEmpty();
    }

    @Test
    void findByUser_whenExists_returnsToken() {
        UserEntity user = persistUser();
        RefreshTokenEntity saved = persistToken(user, "hash-" + UUID.randomUUID(), Instant.now().plusSeconds(3600));

        Optional<RefreshTokenEntity> result = refreshTokenRepo.findByUser(user);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void findByUser_whenUserHasNoToken_returnsEmpty() {
        UserEntity user = persistUser();
        entityManager.flush();

        Optional<RefreshTokenEntity> result = refreshTokenRepo.findByUser(user);

        assertThat(result).isEmpty();
    }

    @Test
    void deleteByUser_removesTokenForThatUser() {
        UserEntity user = persistUser();
        persistToken(user, "hash-" + UUID.randomUUID(), Instant.now().plusSeconds(3600));

        refreshTokenRepo.deleteByUser(user);
        entityManager.flush();
        entityManager.clear();

        assertThat(refreshTokenRepo.findByUser(user)).isEmpty();
    }

    @Test
    void save_persistsCreationTimestampAutomatically() {
        UserEntity user = persistUser();
        RefreshTokenEntity token = RefreshTokenEntity.builder()
                .user(user)
                .tokenHash("hash-" + UUID.randomUUID())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        RefreshTokenEntity saved = refreshTokenRepo.save(token);
        entityManager.flush();
        entityManager.clear();

        RefreshTokenEntity reloaded = refreshTokenRepo.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getCreatedAt()).isNotNull();
    }
}