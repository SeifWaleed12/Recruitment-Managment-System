package com.banquemisr.recruitment.data.repo;

import com.banquemisr.recruitment.data.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepo extends JpaRepository<RoleEntity, String> {

    Optional<RoleEntity> findByRoleName(String roleName);
}
