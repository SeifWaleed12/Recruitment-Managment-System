package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.Authentication.Security.JwtService;
import com.banquemisr.recruitment.Authentication.Security.Property.JwtProperties;
import com.banquemisr.recruitment.data.entity.RefreshTokenEntity;
import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.repo.RefreshTokenRepo;
import com.banquemisr.recruitment.mapper.UserMapper;
import com.banquemisr.recruitment.web.DTOs.respond.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepo refreshTokenRepo;
    private final JwtProperties jwtProperties;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Transactional
    public String createRefreshToken(UserEntity user){

        refreshTokenRepo.deleteByUser(user); // to make sure only one active token per user

        String rawToken= generateRawToken();
        RefreshTokenEntity entity= RefreshTokenEntity.builder()
                .user(user)
                .tokenHash(hashToken(rawToken))
                .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpirationMs()))
                .build();

        refreshTokenRepo.save(entity);
        return rawToken;
    }

    @Transactional
    public AuthResponse refreshAccessToken(String rawRefreshToken){
        RefreshTokenEntity stored= refreshTokenRepo.findByTokenHash(hashToken(rawRefreshToken))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if(stored.getExpiresAt().isBefore(Instant.now())){
            refreshTokenRepo.delete(stored);
            throw new BadCredentialsException("Refresh token expired");
        }

        UserEntity user= stored.getUser();
        String newAccessToken= jwtService.generateAccessToken(user);
        String newRefreshToken= createRefreshToken(user);
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresInMs(jwtProperties.getAccessTokenExpirationMs())
                .user(userMapper.toRespond(user))
                .build();
    }

    public void revokeAllForUser(UserEntity user){
        refreshTokenRepo.deleteByUser(user); // for logout
    }

    private String generateRawToken(){
        return UUID.randomUUID().toString() + UUID.randomUUID().toString();
    }

    private String hashToken(String rawToken){
        try {
            MessageDigest digest= MessageDigest.getInstance("SHA-256");
            byte[] hash= digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        }catch (NoSuchAlgorithmException e){
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
