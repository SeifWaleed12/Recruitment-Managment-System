package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.Authentication.Security.LdapUserService;
import com.banquemisr.recruitment.data.entity.RoleEntity;
import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.repo.UserRepo;
import com.banquemisr.recruitment.exception.DuplicateResourceException;
import com.banquemisr.recruitment.web.DTOs.request.SignUpRequest;
import com.banquemisr.recruitment.web.DTOs.respond.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SignUpService {

    private final UserRepo userRepo;
    private final RoleService roleService;
    private final LdapUserService ldapUserService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponse signup(SignUpRequest request) {
        if (userRepo.existsByUserEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email is already registered: " + request.getEmail());
        }

        RoleEntity role = (request.getRoleId() != null)
                ? roleService.getRoleEntityById(request.getRoleId())
                : roleService.getRoleEntityByName("ROLE_HR");

        UserEntity user = UserEntity.builder()
                .userEmail(request.getEmail())
                .userFname(request.getFirstName())
                .userLname(request.getLastName())
                .role(role)
                .enabled(true)
                .build();

        UserEntity savedUser = userRepo.save(user);


        ldapUserService.createUser(
                savedUser.getUserEmail(),
                savedUser.getUserFname(),
                savedUser.getUserLname(),
                savedUser.getUserEmail(),
                request.getPassword()
        );

        return refreshTokenService.issueTokens(savedUser);
    }
}