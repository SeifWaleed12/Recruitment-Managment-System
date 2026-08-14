package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.data.entity.RoleEntity;
import com.banquemisr.recruitment.data.repo.RoleRepo;
import com.banquemisr.recruitment.exception.DuplicateResourceException;
import com.banquemisr.recruitment.exception.ResourceNotFoundException;
import com.banquemisr.recruitment.mapper.RoleMapper;
import com.banquemisr.recruitment.web.DTOs.request.RoleRequest;
import com.banquemisr.recruitment.web.DTOs.respond.RoleRespond;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepo roleRepo;
    private final RoleMapper roleMapper;

    @Transactional
    public RoleRespond createRole(RoleRequest request) {
        if (roleRepo.findByRoleName(request.getRoleName()).isPresent()) {
            throw new DuplicateResourceException("Role with name " + request.getRoleName() + " already exists!");
        }

        RoleEntity entity = roleMapper.toEntity(request);
        RoleEntity savedEntity = roleRepo.save(entity);
        return roleMapper.toRespond(savedEntity);
    }

    @Transactional(readOnly = true)
    public RoleRespond getRoleById(String roleId) {
        RoleEntity entity = getRoleEntityById(roleId);
        return roleMapper.toRespond(entity);
    }

    @Transactional(readOnly = true)
    public RoleEntity getRoleEntityById(String roleId) {
        return roleRepo.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with ID: " + roleId));
    }

    @Transactional(readOnly = true)
    public RoleEntity getRoleEntityByName(String roleName) {
        return roleRepo.findByRoleName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with name: " + roleName));
    }

    @Transactional(readOnly = true)
    public List<RoleRespond> getAllRoles() {
        return roleRepo.findAll().stream()
                .map(roleMapper::toRespond)
                .collect(Collectors.toList());
    }
}