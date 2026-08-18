package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.Authentication.Security.JwtService;
import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.repo.UserRepo;
import com.banquemisr.recruitment.mapper.UserMapper;
import com.banquemisr.recruitment.web.DTOs.request.LoginRequest;
import com.banquemisr.recruitment.web.DTOs.respond.AuthResponse;
import com.banquemisr.recruitment.web.DTOs.respond.UserRespond;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtService jwtService;

    @Value("${app.jwt.access-token-expiration-ms:300000}")
    private long accessTokenExpirationMs;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepo.findByUserEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (user.getEnabled() != null && !user.getEnabled()) {
            throw new DisabledException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getUserPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        UserRespond userRespond = userMapper.toRespond(user);

        return AuthResponse.builder()
                .accessToken("mock-access-token-" + user.getUserId())
                .refreshToken("mock-refresh-token-" + user.getUserId())
                .expiresInMs(accessTokenExpirationMs)
                .user(userRespond)
                .build();
    }
}
