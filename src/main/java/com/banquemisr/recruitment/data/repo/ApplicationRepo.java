package com.banquemisr.recruitment.data.repo;

import com.banquemisr.recruitment.data.entity.ApplicationEntity;
import com.banquemisr.recruitment.data.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepo extends JpaRepository<ApplicationEntity, String> {

    List<ApplicationEntity> findByJobJobId(String jobId);

    List<ApplicationEntity> findByCandidateCandidateId(String candidateId);

    List<ApplicationEntity> findByStatus(ApplicationStatus status);

    Optional<ApplicationEntity> findByJobJobIdAndCandidateCandidateId(String jobId, String candidateId);

    boolean existsByJobJobIdAndCandidateCandidateId(String jobId, String candidateId);
}
