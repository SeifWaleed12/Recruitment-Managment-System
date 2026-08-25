package com.banquemisr.recruitment.Authentication.Security;

import com.banquemisr.recruitment.Authentication.Security.Property.JwtProperties;
import com.banquemisr.recruitment.data.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    public String generateAccessToken(UserEntity user) {
        String role = (user.getRole() != null) ? user.getRole().name() : null;

        return Jwts.builder()
                .subject(user.getUserEmail())
                .claim("userId", user.getUserId())
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenExpirationMs()))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractRole(String token) {
        Object role = extractClaims(token).get("role");
        return role != null ? role.toString() : null;
    }

    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public String generatePasswordResetToken(UserEntity user) {
        return Jwts.builder()
                .subject(user.getUserEmail())
                .claim("userId", user.getUserId())
                .claim("type", "PASSWORD_RESET")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getResetTokenExpirationMs()))
                .signWith(getSigningKey())
                .compact();
    }

    public String validateAndExtractEmailFromResetToken(String token) {
        Claims claims = extractClaims(token);
        String type = claims.get("type", String.class);
        if (!"PASSWORD_RESET".equals(type)) {
            throw new IllegalArgumentException("Invalid token type for password reset");
        }
        return claims.getSubject();
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}