package com.banquemisr.recruitment.data.repo;

import com.banquemisr.recruitment.data.entity.JobEntity;
import com.banquemisr.recruitment.data.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepo extends JpaRepository<JobEntity, String> {

    List<JobEntity> findByStatus(JobStatus status);

    List<JobEntity> findByDepartment(String department);

    List<JobEntity> findByCreatedByUserId(String createdByUserId);
}
