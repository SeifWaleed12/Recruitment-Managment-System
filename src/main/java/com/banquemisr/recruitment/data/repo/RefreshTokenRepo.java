package com.banquemisr.recruitment.data.repo;

import com.banquemisr.recruitment.data.entity.RefreshTokenEntity;
import com.banquemisr.recruitment.data.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepo extends JpaRepository<RefreshTokenEntity, String> {
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);
    Optional<RefreshTokenEntity> findByUser(UserEntity user);
    void deleteByUser(UserEntity user);
}
