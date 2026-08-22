package com.banquemisr.recruitment.mapper;

import com.banquemisr.recruitment.data.entity.RoleEntity;
import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.web.DTOs.request.UserRequest;
import com.banquemisr.recruitment.web.DTOs.respond.UserRespond;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserEntity toEntity(UserRequest request, RoleEntity role) {
        if (request == null) return null;
        return UserEntity.builder()
                .userEmail(request.getUserEmail())
                .userFname(request.getUserFname())
                .userLname(request.getUserLname())
                .role(role)
                .enabled(request.getEnabled())
                .build();
    }

    public UserRespond toRespond(UserEntity entity) {
        if (entity == null) return null;
        return UserRespond.builder()
                .userId(entity.getUserId())
                .userEmail(entity.getUserEmail())
                .userFname(entity.getUserFname())
                .userLname(entity.getUserLname())
                .roleId(entity.getRole() != null ? entity.getRole().getRoleId() : null)
                .roleName(entity.getRole() != null ? entity.getRole().getRoleName() : null)
                .enabled(entity.getEnabled())
                .build();
    }
}