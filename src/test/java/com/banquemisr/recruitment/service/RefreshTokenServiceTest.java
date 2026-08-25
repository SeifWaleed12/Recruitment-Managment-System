package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.Authentication.Security.JwtService;
import com.banquemisr.recruitment.Authentication.Security.Property.JwtProperties;
import com.banquemisr.recruitment.data.entity.RefreshTokenEntity;
import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.repo.RefreshTokenRepo;
import com.banquemisr.recruitment.mapper.UserMapper;
import com.banquemisr.recruitment.web.DTOs.respond.AuthResponse;
import com.banquemisr.recruitment.web.DTOs.respond.UserRespond;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepo refreshTokenRepo;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private UserEntity user() {
        return UserEntity.builder().userId("user-1").userEmail("jane@example.com").build();
    }

    // ---------- issueTokens ----------

    @Test
    void issueTokens_buildsAuthResponseWithAccessAndRefreshTokens() {

        UserEntity user = user();
        UserRespond userRespond = UserRespond.builder().userId("user-1").build();

        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(refreshTokenRepo.findByUser(user)).thenReturn(Optional.empty());
        when(refreshTokenRepo.save(any(RefreshTokenEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtProperties.getAccessTokenExpirationMs()).thenReturn(300000L);
        when(jwtProperties.getRefreshTokenExpirationMs()).thenReturn(86400000L);
        when(userMapper.toRespond(user)).thenReturn(userRespond);

        AuthResponse result = refreshTokenService.issueTokens(user);

        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isNotBlank();
        assertThat(result.getExpiresInMs()).isEqualTo(300000L);
        assertThat(result.getUser()).isEqualTo(userRespond);
    }

    // ---------- createRefreshToken ----------

    @Test
    void createRefreshToken_whenNoExistingToken_createsNewEntity() {

        UserEntity user = user();

        when(refreshTokenRepo.findByUser(user)).thenReturn(Optional.empty());
        when(jwtProperties.getRefreshTokenExpirationMs()).thenReturn(86400000L);
        when(refreshTokenRepo.save(any(RefreshTokenEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String rawToken = refreshTokenService.createRefreshToken(user);

        assertThat(rawToken).isNotBlank();

        ArgumentCaptor<RefreshTokenEntity> captor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(refreshTokenRepo).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getTokenHash()).isNotBlank();
    }

    @Test
    void createRefreshToken_whenExistingToken_updatesHashAndExpiry() {

        UserEntity user = user();
        RefreshTokenEntity existing = RefreshTokenEntity.builder()
                .id("rt-1")
                .user(user)
                .tokenHash("old-hash")
                .expiresAt(Instant.now().minusSeconds(60))
                .build();

        when(refreshTokenRepo.findByUser(user)).thenReturn(Optional.of(existing));
        when(jwtProperties.getRefreshTokenExpirationMs()).thenReturn(86400000L);
        when(refreshTokenRepo.save(existing)).thenReturn(existing);

        refreshTokenService.createRefreshToken(user);

        assertThat(existing.getTokenHash()).isNotEqualTo("old-hash");
        assertThat(existing.getExpiresAt()).isAfter(Instant.now());
        verify(refreshTokenRepo).save(existing);
    }

    // ---------- refreshAccessToken ----------

    @Test
    void refreshAccessToken_whenTokenNull_throwsBadCredentialsException() {

        assertThatThrownBy(() -> refreshTokenService.refreshAccessToken(null))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(refreshTokenRepo);
    }

    @Test
    void refreshAccessToken_whenTokenBlank_throwsBadCredentialsException() {

        assertThatThrownBy(() -> refreshTokenService.refreshAccessToken("   "))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(refreshTokenRepo);
    }

    @Test
    void refreshAccessToken_whenTokenNotFound_throwsBadCredentialsException() {

        when(refreshTokenRepo.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.refreshAccessToken("some-raw-token"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refreshAccessToken_whenTokenExpired_deletesAndThrowsBadCredentialsException() {

        RefreshTokenEntity expired = RefreshTokenEntity.builder()
                .id("rt-1")
                .user(user())
                .tokenHash("hash")
                .expiresAt(Instant.now().minusSeconds(10))
                .build();

        when(refreshTokenRepo.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> refreshTokenService.refreshAccessToken("raw-token"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("expired");

        verify(refreshTokenRepo).delete(expired);
    }

    @Test
    void refreshAccessToken_whenValid_issuesNewTokens() {

        UserEntity user = user();
        RefreshTokenEntity stored = RefreshTokenEntity.builder()
                .id("rt-1")
                .user(user)
                .tokenHash("hash")
                .expiresAt(Instant.now().plusSeconds(600))
                .build();

        when(refreshTokenRepo.findByTokenHash(anyString())).thenReturn(Optional.of(stored));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
        when(refreshTokenRepo.findByUser(user)).thenReturn(Optional.of(stored));
        when(refreshTokenRepo.save(any(RefreshTokenEntity.class))).thenReturn(stored);
        when(jwtProperties.getAccessTokenExpirationMs()).thenReturn(300000L);
        when(jwtProperties.getRefreshTokenExpirationMs()).thenReturn(86400000L);
        when(userMapper.toRespond(user)).thenReturn(UserRespond.builder().userId("user-1").build());

        AuthResponse result = refreshTokenService.refreshAccessToken(" raw-token ");

        assertThat(result.getAccessToken()).isEqualTo("new-access-token");
    }

    // ---------- revokeAllForUser ----------

    @Test
    void revokeAllForUser_deletesTokensForUser() {

        UserEntity user = user();

        refreshTokenService.revokeAllForUser(user);

        verify(refreshTokenRepo).deleteByUser(user);
    }
}