package com.banquemisr.recruitment.mapper;

import com.banquemisr.recruitment.data.entity.RoleEntity;
import com.banquemisr.recruitment.web.DTOs.request.RoleRequest;
import com.banquemisr.recruitment.web.DTOs.respond.RoleRespond;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper {

    public RoleEntity toEntity(RoleRequest request) {
        if (request == null) return null;
        return RoleEntity.builder()
                .roleName(request.getRoleName())
                .build();
    }

    public RoleRespond toRespond(RoleEntity entity) {
        if (entity == null) return null;
        return RoleRespond.builder()
                .roleId(entity.getRoleId())
                .roleName(entity.getRoleName())
                .build();
    }
}
