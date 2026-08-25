package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.Authentication.Security.LdapUserService;
import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.enums.Role;
import com.banquemisr.recruitment.data.repo.UserRepo;
import com.banquemisr.recruitment.exception.DuplicateResourceException;
import com.banquemisr.recruitment.exception.ResourceNotFoundException;
import com.banquemisr.recruitment.mapper.UserMapper;
import com.banquemisr.recruitment.web.DTOs.request.UserRequest;
import com.banquemisr.recruitment.web.DTOs.respond.UserRespond;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private UserMapper userMapper;

    @Mock
    private LdapUserService ldapUserService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private UserService userService;

    private UserRequest.UserRequestBuilder baseRequest() {
        return UserRequest.builder()
                .userEmail("jane@example.com")
                .userPassword("password123")
                .userFname("Jane")
                .userLname("Doe");
    }

    // ---------- createUser / createInterviewer / createHr / createAdmin ----------

    @Test
    void createUser_whenRoleNotProvided_defaultsToInterviewer() {

        UserRequest request = baseRequest().build();
        UserEntity entity = UserEntity.builder()
                .userId("user-1")
                .userEmail("jane@example.com")
                .userFname("Jane")
                .userLname("Doe")
                .build();
        UserRespond respond = UserRespond.builder().userId("user-1").build();

        when(userRepo.existsByUserEmail("jane@example.com")).thenReturn(false);
        when(userMapper.toEntity(request, Role.ROLE_INTERVIEWER)).thenReturn(entity);
        when(userRepo.save(entity)).thenReturn(entity);
        when(userMapper.toRespond(entity)).thenReturn(respond);

        UserRespond result = userService.createUser(request);

        assertThat(result.getUserId()).isEqualTo("user-1");
        verify(ldapUserService).createUser("jane@example.com", "Jane", "Doe", "jane@example.com", "password123");
    }

    @Test
    void createUser_whenRoleProvided_usesProvidedRole() {

        UserRequest request = baseRequest().role(Role.ROLE_ADMIN).build();
        UserEntity entity = UserEntity.builder().userId("user-1").userEmail("jane@example.com").build();
        UserRespond respond = UserRespond.builder().userId("user-1").build();

        when(userRepo.existsByUserEmail("jane@example.com")).thenReturn(false);
        when(userMapper.toEntity(request, Role.ROLE_ADMIN)).thenReturn(entity);
        when(userRepo.save(entity)).thenReturn(entity);
        when(userMapper.toRespond(entity)).thenReturn(respond);

        UserRespond result = userService.createUser(request);

        assertThat(result.getUserId()).isEqualTo("user-1");
    }

    @Test
    void createUser_whenEmailAlreadyExists_throwsDuplicateResourceException() {

        UserRequest request = baseRequest().build();

        when(userRepo.existsByUserEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepo, never()).save(any());
        verifyNoInteractions(ldapUserService);
    }

    @Test
    void createInterviewer_savesWithInterviewerRole() {

        UserRequest request = baseRequest().build();
        UserEntity entity = UserEntity.builder().userId("user-1").userEmail("jane@example.com").build();

        when(userRepo.existsByUserEmail("jane@example.com")).thenReturn(false);
        when(userMapper.toEntity(request, Role.ROLE_INTERVIEWER)).thenReturn(entity);
        when(userRepo.save(entity)).thenReturn(entity);
        when(userMapper.toRespond(entity)).thenReturn(UserRespond.builder().userId("user-1").build());

        userService.createInterviewer(request);

        verify(userMapper).toEntity(request, Role.ROLE_INTERVIEWER);
    }

    @Test
    void createHr_savesWithHrRole() {

        UserRequest request = baseRequest().build();
        UserEntity entity = UserEntity.builder().userId("user-1").userEmail("jane@example.com").build();

        when(userRepo.existsByUserEmail("jane@example.com")).thenReturn(false);
        when(userMapper.toEntity(request, Role.ROLE_HR)).thenReturn(entity);
        when(userRepo.save(entity)).thenReturn(entity);
        when(userMapper.toRespond(entity)).thenReturn(UserRespond.builder().userId("user-1").build());

        userService.createHr(request);

        verify(userMapper).toEntity(request, Role.ROLE_HR);
    }

    @Test
    void createAdmin_savesWithAdminRole() {

        UserRequest request = baseRequest().build();
        UserEntity entity = UserEntity.builder().userId("user-1").userEmail("jane@example.com").build();

        when(userRepo.existsByUserEmail("jane@example.com")).thenReturn(false);
        when(userMapper.toEntity(request, Role.ROLE_ADMIN)).thenReturn(entity);
        when(userRepo.save(entity)).thenReturn(entity);
        when(userMapper.toRespond(entity)).thenReturn(UserRespond.builder().userId("user-1").build());

        userService.createAdmin(request);

        verify(userMapper).toEntity(request, Role.ROLE_ADMIN);
    }

    // ---------- changeUserRole ----------

    @Test
    void changeUserRole_updatesRoleAndReturnsRespond() {

        UserEntity entity = UserEntity.builder().userId("user-1").role(Role.ROLE_HR).build();
        UserRespond respond = UserRespond.builder().userId("user-1").role(Role.ROLE_ADMIN).build();

        when(userRepo.findById("user-1")).thenReturn(Optional.of(entity));
        when(userRepo.save(entity)).thenReturn(entity);
        when(userMapper.toRespond(entity)).thenReturn(respond);

        UserRespond result = userService.changeUserRole("user-1", Role.ROLE_ADMIN);

        assertThat(entity.getRole()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(result.getRole()).isEqualTo(Role.ROLE_ADMIN);
    }

    // ---------- getUserById / getUserEntityById ----------

    @Test
    void getUserById_whenExists_returnsRespond() {

        UserEntity entity = UserEntity.builder().userId("user-1").build();
        UserRespond respond = UserRespond.builder().userId("user-1").build();

        when(userRepo.findById("user-1")).thenReturn(Optional.of(entity));
        when(userMapper.toRespond(entity)).thenReturn(respond);

        UserRespond result = userService.getUserById("user-1");

        assertThat(result.getUserId()).isEqualTo("user-1");
    }

    @Test
    void getUserEntityById_whenNotFound_throwsResourceNotFoundException() {

        when(userRepo.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserEntityById("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- getAllUsers ----------

    @Test
    void getAllUsers_returnsMappedList() {

        UserEntity entity = UserEntity.builder().userId("user-1").build();
        UserRespond respond = UserRespond.builder().userId("user-1").build();

        when(userRepo.findAll()).thenReturn(List.of(entity));
        when(userMapper.toRespond(entity)).thenReturn(respond);

        List<UserRespond> result = userService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo("user-1");
    }

    // ---------- updateUser ----------

    @Test
    void updateUser_whenExists_updatesFieldsAndReturnsRespond() {

        UserEntity entity = UserEntity.builder()
                .userId("user-1")
                .userFname("Old")
                .userLname("Name")
                .userEmail("old@example.com")
                .role(Role.ROLE_HR)
                .build();

        UserRequest request = baseRequest().userFname("New").userLname("Name2")
                .userEmail("new@example.com").role(Role.ROLE_ADMIN).build();

        when(userRepo.findById("user-1")).thenReturn(Optional.of(entity));
        when(userRepo.save(entity)).thenReturn(entity);
        when(userMapper.toRespond(entity)).thenReturn(UserRespond.builder().userId("user-1").build());

        UserRespond result = userService.updateUser("user-1", request);

        assertThat(entity.getUserFname()).isEqualTo("New");
        assertThat(entity.getUserLname()).isEqualTo("Name2");
        assertThat(entity.getUserEmail()).isEqualTo("new@example.com");
        assertThat(entity.getRole()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(result.getUserId()).isEqualTo("user-1");
    }

    @Test
    void updateUser_whenRoleNotProvided_keepsExistingRole() {

        UserEntity entity = UserEntity.builder()
                .userId("user-1")
                .userFname("Old")
                .userLname("Name")
                .userEmail("old@example.com")
                .role(Role.ROLE_HR)
                .build();

        UserRequest request = UserRequest.builder()
                .userFname("New")
                .userLname("Name")
                .userEmail("new@example.com")
                .userPassword("password123")
                .build();

        when(userRepo.findById("user-1")).thenReturn(Optional.of(entity));
        when(userRepo.save(entity)).thenReturn(entity);
        when(userMapper.toRespond(entity)).thenReturn(UserRespond.builder().userId("user-1").build());

        userService.updateUser("user-1", request);

        assertThat(entity.getRole()).isEqualTo(Role.ROLE_HR);
    }

    @Test
    void updateUser_whenNotFound_throwsResourceNotFoundException() {

        when(userRepo.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser("missing", baseRequest().build()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepo, never()).save(any());
    }

    // ---------- changePassword ----------

    @Test
    void changePassword_whenOldPasswordCorrect_updatesLdapPassword() {

        UserEntity entity = UserEntity.builder().userId("user-1").userEmail("jane@example.com").build();

        when(userRepo.findById("user-1")).thenReturn(Optional.of(entity));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(org.springframework.security.core.Authentication.class));

        userService.changePassword("user-1", "oldpass", "newpass");

        verify(ldapUserService).updatePassword("jane@example.com", "newpass");
    }

    @Test
    void changePassword_whenOldPasswordIncorrect_throwsIllegalArgumentException() {

        UserEntity entity = UserEntity.builder().userId("user-1").userEmail("jane@example.com").build();

        when(userRepo.findById("user-1")).thenReturn(Optional.of(entity));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> userService.changePassword("user-1", "wrongpass", "newpass"))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(ldapUserService);
    }

    // ---------- toggleUserEnabled ----------

    @Test
    void toggleUserEnabled_updatesEnabledFlag() {

        UserEntity entity = UserEntity.builder().userId("user-1").enabled(true).build();

        when(userRepo.findById("user-1")).thenReturn(Optional.of(entity));
        when(userRepo.save(entity)).thenReturn(entity);
        when(userMapper.toRespond(entity)).thenReturn(UserRespond.builder().userId("user-1").enabled(false).build());

        UserRespond result = userService.toggleUserEnabled("user-1", false);

        assertThat(entity.getEnabled()).isFalse();
        assertThat(result.getEnabled()).isFalse();
    }

    // ---------- deleteUser ----------

    @Test
    void deleteUser_whenExists_deletesFromDbAndLdap() {

        UserEntity entity = UserEntity.builder().userId("user-1").userEmail("jane@example.com").build();

        when(userRepo.findById("user-1")).thenReturn(Optional.of(entity));

        userService.deleteUser("user-1");

        verify(userRepo).deleteById("user-1");
        verify(ldapUserService).deleteUser("jane@example.com");
    }

    @Test
    void deleteUser_whenNotFound_throwsResourceNotFoundException() {

        when(userRepo.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser("missing"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepo, never()).deleteById(any());
        verifyNoInteractions(ldapUserService);
    }
}