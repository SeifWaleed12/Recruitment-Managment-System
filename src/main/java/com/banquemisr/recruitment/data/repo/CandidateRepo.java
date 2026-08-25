package com.banquemisr.recruitment.data.repo;

import com.banquemisr.recruitment.data.entity.CandidateEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandidateRepo extends JpaRepository<CandidateEntity, String>, JpaSpecificationExecutor<CandidateEntity> {

    Optional<CandidateEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    @Override
    @EntityGraph(attributePaths = {"skills", "tags", "createdBy"})
    Optional<CandidateEntity> findById(String id);

    @Override
    @EntityGraph(attributePaths = {"skills", "tags", "createdBy"})
    Page<CandidateEntity> findAll(Specification<CandidateEntity> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"skills", "tags", "createdBy"})
    Page<CandidateEntity> findAll(Pageable pageable);

    @Query("SELECT DISTINCT c FROM CandidateEntity c LEFT JOIN FETCH c.skills LEFT JOIN FETCH c.tags LEFT JOIN FETCH c.createdBy")
    List<CandidateEntity> findAllWithSkillsAndTags();
}
