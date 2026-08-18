package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.Authentication.Security.JwtService;
import com.banquemisr.recruitment.Authentication.Security.Property.JwtProperties;
import com.banquemisr.recruitment.data.entity.RoleEntity;
import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.repo.UserRepo;
import com.banquemisr.recruitment.exception.DuplicateResourceException;
import com.banquemisr.recruitment.mapper.UserMapper;
import com.banquemisr.recruitment.web.DTOs.request.SignUpRequest;
import com.banquemisr.recruitment.web.DTOs.respond.AuthResponse;
import com.banquemisr.recruitment.web.DTOs.respond.UserRespond;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SignUpService {

    private final UserRepo userRepo;
    private final RoleService roleService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;

    @Transactional
    public AuthResponse signup(SignUpRequest request) {
        if (userRepo.existsByUserEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email is already registered: " + request.getEmail());
        }

        RoleEntity role = null;
        if (request.getRoleId() != null) {
            role = roleService.getRoleEntityById(request.getRoleId());
        } else {
            role = roleService.getRoleEntityByName("ROLE_HR");
        }

        UserEntity user = UserEntity.builder()
                .userEmail(request.getEmail())
                .userPassword(passwordEncoder.encode(request.getPassword()))
                .userFname(request.getFirstName())
                .userLname(request.getLastName())
                .role(role)
                .enabled(true)
                .build();

        UserEntity savedUser = userRepo.save(user);
        String accessToken= jwtService.generateAccessToken(savedUser);
        String refreshToken= refreshTokenService.createRefreshToken(savedUser);
        UserRespond userRespond = userMapper.toRespond(savedUser);

        return AuthResponse.builder()
                .accessToken("mock-access-token-" + savedUser.getUserId())
                .refreshToken("mock-refresh-token-" + savedUser.getUserId())
                .expiresInMs(jwtProperties.getAccessTokenExpirationMs())
                .user(userRespond)
                .build();
    }
}
