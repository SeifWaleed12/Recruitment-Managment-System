package com.banquemisr.recruitment.data.repo;

import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.enums.Role;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class UserRepoTest {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private EntityManager entityManager;

    private UserEntity persistUser(String email) {
        UserEntity user = UserEntity.builder()
                .userEmail(email)
                .userFname("Sara")
                .userLname("Ahmed")
                .role(Role.ROLE_HR)
                .build();
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    @Test
    void findByUserEmail_whenExists_returnsUser() {
        String email = "sara-" + UUID.randomUUID() + "@example.com";
        UserEntity saved = persistUser(email);

        Optional<UserEntity> result = userRepo.findByUserEmail(email);

        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(saved.getUserId());
    }

    @Test
    void findByUserEmail_whenNotExists_returnsEmpty() {
        Optional<UserEntity> result = userRepo.findByUserEmail("nobody@example.com");

        assertThat(result).isEmpty();
    }

    @Test
    void existsByUserEmail_whenExists_returnsTrue() {
        String email = "sara-" + UUID.randomUUID() + "@example.com";
        persistUser(email);

        assertThat(userRepo.existsByUserEmail(email)).isTrue();
    }

    @Test
    void existsByUserEmail_whenNotExists_returnsFalse() {
        assertThat(userRepo.existsByUserEmail("nobody@example.com")).isFalse();
    }

    @Test
    void save_withDuplicateEmail_violatesUniqueConstraint() {
        String email = "sara-" + UUID.randomUUID() + "@example.com";
        persistUser(email);

        UserEntity duplicate = UserEntity.builder()
                .userEmail(email)
                .userFname("Omar")
                .userLname("Khaled")
                .role(Role.ROLE_INTERVIEWER)
                .build();

        assertThatThrownBy(() -> userRepo.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void save_persistsEnabledDefaultAsTrue() {
        UserEntity user = UserEntity.builder()
                .userEmail("new-" + UUID.randomUUID() + "@example.com")
                .userFname("New")
                .userLname("User")
                .role(Role.ROLE_INTERVIEWER)
                .build();

        UserEntity saved = userRepo.save(user);
        entityManager.flush();
        entityManager.clear();

        UserEntity reloaded = userRepo.findById(saved.getUserId()).orElseThrow();
        assertThat(reloaded.getEnabled()).isTrue();
    }
}