package com.banquemisr.recruitment.mapper;

import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.enums.Role;
import com.banquemisr.recruitment.web.DTOs.request.UserRequest;
import com.banquemisr.recruitment.web.DTOs.respond.UserRespond;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserEntity toEntity(UserRequest request, Role role) {
        if (request == null) return null;
        return UserEntity.builder()
                .userEmail(request.getUserEmail())
                .userPassword(request.getUserPassword())
                .userFname(request.getUserFname())
                .userLname(request.getUserLname())
                .role(role != null ? role : request.getRole())
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
                .role(entity.getRole())
                .roleName(entity.getRole() != null ? entity.getRole().name() : null)
                .enabled(entity.getEnabled())
                .build();
    }
}
