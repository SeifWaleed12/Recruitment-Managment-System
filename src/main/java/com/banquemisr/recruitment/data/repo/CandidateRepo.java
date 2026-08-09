package com.banquemisr.recruitment.data.repo;

import com.banquemisr.recruitment.data.entity.CandidateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandidateRepo extends JpaRepository<CandidateEntity, String> {

    Optional<CandidateEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
