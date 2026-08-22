package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.enums.Role;
import com.banquemisr.recruitment.data.repo.UserRepo;
import com.banquemisr.recruitment.web.DTOs.request.LoginRequest;
import com.banquemisr.recruitment.web.DTOs.respond.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final AuthenticationManager authenticationManager;
    private final UserRepo userRepo;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException ex) {
            throw new BadCredentialsException("Invalid credentials");
        }

        UserEntity user = userRepo.findByUserEmail(request.getEmail())
                .orElseGet(() -> syncLdapUserToDatabase(authentication, request.getEmail()));

        if (user.getEnabled() != null && !user.getEnabled()) {
            throw new DisabledException("Account is disabled");
        }

        return refreshTokenService.issueTokens(user);
    }

    private UserEntity syncLdapUserToDatabase(Authentication authentication, String email) {
        String authority = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .findFirst()
                .orElse("ROLE_HR");

        Role role;
        try {
            role = Role.valueOf(authority.toUpperCase());
        } catch (Exception e) {
            role = Role.ROLE_HR;
        }

        String firstName = "LDAP";
        String lastName = "User";
        if (email != null && email.contains("@")) {
            String namePart = email.substring(0, email.indexOf('@'));
            if (namePart.contains(".")) {
                String[] parts = namePart.split("\\.");
                firstName = capitalize(parts[0]);
                lastName = capitalize(parts[1]);
            } else {
                firstName = capitalize(namePart);
            }
        }

        UserEntity newUser = UserEntity.builder()
                .userEmail(email)
                .userFname(firstName)
                .userLname(lastName)
                .role(role)
                .enabled(true)
                .build();

        return userRepo.save(newUser);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
