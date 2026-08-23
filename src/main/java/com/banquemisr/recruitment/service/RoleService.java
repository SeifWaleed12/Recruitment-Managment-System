package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.data.enums.Role;
import com.banquemisr.recruitment.web.DTOs.respond.RoleRespond;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    public List<RoleRespond> getAllRoles() {
        return Arrays.stream(Role.values())
                .map(role -> RoleRespond.builder()
                        .role(role)
                        .roleName(role.name())
                        .build())
                .collect(Collectors.toList());
    }

    public RoleRespond getRoleByName(String name) {
        Role role = Role.valueOf(name.toUpperCase());
        return RoleRespond.builder()
                .role(role)
                .roleName(role.name())
                .build();
    }
}