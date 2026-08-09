package com.banquemisr.recruitment.data.repo;

import com.banquemisr.recruitment.data.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<UserEntity, String> {

    Optional<UserEntity> findByUserEmail(String userEmail);

    boolean existsByUserEmail(String userEmail);
}
