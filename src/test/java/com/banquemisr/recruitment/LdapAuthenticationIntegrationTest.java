package com.banquemisr.recruitment;

import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.enums.Role;
import com.banquemisr.recruitment.data.repo.UserRepo;
import com.banquemisr.recruitment.web.DTOs.request.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LdapAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("Should authenticate LDAP HR user, auto-provision shadow user, and issue JWT tokens")
    void testLdapAuthenticationSuccess_HR() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("hr@banquemisr.com")
                .password("password123")
                .build();

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.userEmail").value("hr@banquemisr.com"))
                .andExpect(jsonPath("$.user.roleName").value("ROLE_HR"))
                .andReturn();

        UserEntity dbUser = userRepo.findByUserEmail("hr@banquemisr.com").orElse(null);
        assertThat(dbUser).isNotNull();
        assertThat(dbUser.getUserEmail()).isEqualTo("hr@banquemisr.com");
        assertThat(dbUser.getRole()).isEqualTo(Role.ROLE_HR);
    }

    @Test
    @DisplayName("Should authenticate LDAP Interviewer user and assign ROLE_INTERVIEWER")
    void testLdapAuthenticationSuccess_Interviewer() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("interviewer@banquemisr.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.userEmail").value("interviewer@banquemisr.com"))
                .andExpect(jsonPath("$.user.roleName").value("ROLE_INTERVIEWER"));
    }

    @Test
    @DisplayName("Should fail authentication when invalid LDAP password is provided")
    void testLdapAuthenticationFailure_BadPassword() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("hr@banquemisr.com")
                .password("wrong_password")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should fallback to Local DB authentication for non-LDAP database users")
    void testDatabaseFallbackAuthentication_LocalUser() throws Exception {
        String localEmail = "localadmin@company.com";

        if (userRepo.findByUserEmail(localEmail).isEmpty()) {
            userRepo.save(UserEntity.builder()
                    .userEmail(localEmail)
                    .userPassword(passwordEncoder.encode("LocalSecret123!"))
                    .userFname("Local")
                    .userLname("Admin")
                    .role(Role.ROLE_ADMIN)
                    .enabled(true)
                    .build());
        }

        LoginRequest request = LoginRequest.builder()
                .email(localEmail)
                .password("LocalSecret123!")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.userEmail").value(localEmail))
                .andExpect(jsonPath("$.user.roleName").value("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("Should use LDAP JWT token to access protected endpoint")
    void testProtectedEndpointAccessWithLdapToken() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("hr@banquemisr.com")
                .password("password123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = loginResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseJson).get("accessToken").asText();

        mockMvc.perform(get("/api/v1/candidates")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
