package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.enums.Role;
import com.banquemisr.recruitment.data.repo.UserRepo;
import com.banquemisr.recruitment.web.DTOs.request.LoginRequest;
import com.banquemisr.recruitment.web.DTOs.respond.AuthResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepo userRepo;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private LoginService loginService;

    private LoginRequest loginRequest(String email, String password) {
        return LoginRequest.builder().email(email).password(password).build();
    }

    // ---------- authentication failure ----------

    @Test
    void login_whenAuthenticationFails_throwsBadCredentialsException() {
        LoginRequest request = loginRequest("jane@example.com", "wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("bad"));

        assertThatThrownBy(() -> loginService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(userRepo, refreshTokenService);
    }

    // ---------- existing user in DB ----------

    @Test
    void login_whenUserExistsAndEnabled_issuesTokens() {
        LoginRequest request = loginRequest("jane@example.com", "correctpassword");
        UserEntity existingUser = UserEntity.builder()
                .userId("user-1")
                .userEmail("jane@example.com")
                .enabled(true)
                .role(Role.ROLE_HR)
                .build();
        AuthResponse expectedResponse = AuthResponse.builder().accessToken("token-abc").build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepo.findByUserEmail("jane@example.com")).thenReturn(Optional.of(existingUser));
        when(refreshTokenService.issueTokens(existingUser)).thenReturn(expectedResponse);

        AuthResponse result = loginService.login(request);

        assertThat(result.getAccessToken()).isEqualTo("token-abc");
        verify(userRepo, never()).save(any()); // existing user — no LDAP sync should happen
    }

    @Test
    void login_whenUserDisabled_throwsDisabledException() {
        LoginRequest request = loginRequest("jane@example.com", "correctpassword");
        UserEntity disabledUser = UserEntity.builder()
                .userEmail("jane@example.com")
                .enabled(false)
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepo.findByUserEmail("jane@example.com")).thenReturn(Optional.of(disabledUser));

        assertThatThrownBy(() -> loginService.login(request))
                .isInstanceOf(DisabledException.class);

        verifyNoInteractions(refreshTokenService);
    }

    // ---------- new user: LDAP sync path ----------

    @Test
    void login_whenUserNotInDb_createsUserWithRoleFromLdapAuthorities() {
        LoginRequest request = loginRequest("omar.khaled@example.com", "correctpassword");
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_INTERVIEWER"));

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        doReturn(authorities).when(authentication).getAuthorities();
        when(userRepo.findByUserEmail("omar.khaled@example.com")).thenReturn(Optional.empty());
        when(userRepo.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshTokenService.issueTokens(any(UserEntity.class)))
                .thenReturn(AuthResponse.builder().accessToken("token-xyz").build());

        loginService.login(request);

        verify(userRepo).save(argThat(saved ->
                saved.getUserEmail().equals("omar.khaled@example.com")
                        && saved.getRole() == Role.ROLE_INTERVIEWER
                        && saved.getUserFname().equals("Omar")
                        && saved.getUserLname().equals("Khaled")
                        && saved.getEnabled()
        ));
    }

    @Test
    void login_whenUserNotInDb_noRoleAuthority_defaultsToHr() {
        LoginRequest request = loginRequest("newuser@example.com", "correctpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        doReturn(List.of()).when(authentication).getAuthorities(); // no ROLE_ authority at all
        when(userRepo.findByUserEmail("newuser@example.com")).thenReturn(Optional.empty());
        when(userRepo.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshTokenService.issueTokens(any(UserEntity.class)))
                .thenReturn(AuthResponse.builder().accessToken("token-xyz").build());

        loginService.login(request);

        verify(userRepo).save(argThat(saved -> saved.getRole() == Role.ROLE_HR));
    }

    @Test
    void login_whenUserNotInDb_emailWithoutDot_usesWholeLocalPartAsFirstName() {
        LoginRequest request = loginRequest("admin@example.com", "correctpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();
        when(userRepo.findByUserEmail("admin@example.com")).thenReturn(Optional.empty());
        when(userRepo.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshTokenService.issueTokens(any(UserEntity.class)))
                .thenReturn(AuthResponse.builder().accessToken("token-xyz").build());

        loginService.login(request);

        verify(userRepo).save(argThat(saved ->
                saved.getUserFname().equals("Admin") && saved.getUserLname().equals("User")
        ));
    }
}