package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.enums.Role;
import com.banquemisr.recruitment.data.repo.UserRepo;
import com.banquemisr.recruitment.exception.DuplicateResourceException;
import com.banquemisr.recruitment.exception.ResourceNotFoundException;
import com.banquemisr.recruitment.mapper.UserMapper;
import com.banquemisr.recruitment.web.DTOs.request.UserRequest;
import com.banquemisr.recruitment.web.DTOs.respond.UserRespond;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepo userRepo;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserRespond createUser(UserRequest request) {
        Role targetRole = request.getRole() != null ? request.getRole() : Role.ROLE_INTERVIEWER;
        return saveUserWithRole(request, targetRole);
    }

    @Transactional
    public UserRespond createInterviewer(UserRequest request) {
        return saveUserWithRole(request, Role.ROLE_INTERVIEWER);
    }

    @Transactional
    public UserRespond createHr(UserRequest request) {
        return saveUserWithRole(request, Role.ROLE_HR);
    }

    @Transactional
    public UserRespond createAdmin(UserRequest request) {
        return saveUserWithRole(request, Role.ROLE_ADMIN);
    }

    private UserRespond saveUserWithRole(UserRequest request, Role role) {
        if (userRepo.existsByUserEmail(request.getUserEmail())) {
            throw new DuplicateResourceException("User with email " + request.getUserEmail() + " already exists!");
        }

        UserEntity entity = userMapper.toEntity(request, role);
        entity.setUserPassword(passwordEncoder.encode(request.getUserPassword()));

        UserEntity savedEntity = userRepo.save(entity);
        return userMapper.toRespond(savedEntity);
    }

    @Transactional
    public UserRespond changeUserRole(String userId, Role newRole) {
        UserEntity user = getUserEntityById(userId);
        user.setRole(newRole);
        UserEntity updatedUser = userRepo.save(user);
        return userMapper.toRespond(updatedUser);
    }

    @Transactional(readOnly = true)
    public UserRespond getUserById(String userId) {
        UserEntity entity = getUserEntityById(userId);
        return userMapper.toRespond(entity);
    }

    @Transactional(readOnly = true)
    public UserEntity getUserEntityById(String userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }

    @Transactional(readOnly = true)
    public List<UserRespond> getAllUsers() {
        return userRepo.findAll().stream()
                .map(userMapper::toRespond)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserRespond updateUser(String userId, UserRequest request) {
        UserEntity existingUser = getUserEntityById(userId);

        existingUser.setUserFname(request.getUserFname());
        existingUser.setUserLname(request.getUserLname());
        existingUser.setUserEmail(request.getUserEmail());

        if (request.getRole() != null) {
            existingUser.setRole(request.getRole());
        }

        UserEntity updatedUser = userRepo.save(existingUser);
        return userMapper.toRespond(updatedUser);
    }

    @Transactional
    public void changePassword(String userId, String oldPassword, String newPassword) {
        UserEntity user = getUserEntityById(userId);

        if (!passwordEncoder.matches(oldPassword, user.getUserPassword())) {
            throw new IllegalArgumentException("Incorrect old password provided!");
        }

        user.setUserPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);
    }

    @Transactional
    public UserRespond toggleUserEnabled(String userId, Boolean enabled) {
        UserEntity user = getUserEntityById(userId);
        user.setEnabled(enabled);
        UserEntity updatedUser = userRepo.save(user);
        return userMapper.toRespond(updatedUser);
    }

    @Transactional
    public void deleteUser(String userId) {
        if (!userRepo.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }
        userRepo.deleteById(userId);
    }
}