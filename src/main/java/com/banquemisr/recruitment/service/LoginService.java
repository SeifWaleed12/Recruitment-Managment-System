package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.repo.UserRepo;
import com.banquemisr.recruitment.web.DTOs.request.LoginRequest;
import com.banquemisr.recruitment.web.DTOs.respond.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserRepo userRepo;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            // Delegates to ldapAuthenticationProvider: performs an LDAP bind with the
            // submitted email/password. Throws on failure (BadCredentialsException by default).
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException ex) {
            throw new BadCredentialsException("Invalid credentials");
        }

        UserEntity user = userRepo.findByUserEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (user.getEnabled() != null && !user.getEnabled()) {
            throw new DisabledException("Account is disabled");
        }

        return refreshTokenService.issueTokens(user);
    }
}