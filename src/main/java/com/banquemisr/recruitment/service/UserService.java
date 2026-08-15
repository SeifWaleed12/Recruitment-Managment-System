package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.data.entity.RoleEntity;
import com.banquemisr.recruitment.data.entity.UserEntity;
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
    private final RoleService roleService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;


    @Transactional
    public UserRespond createUser(UserRequest request) {
        if (userRepo.existsByUserEmail(request.getUserEmail())) {
            throw new DuplicateResourceException("User with email " + request.getUserEmail() + " already exists!");
        }

        // Fetch RoleEntity via RoleService
        RoleEntity role = roleService.getRoleEntityById(request.getRoleId());

        // Map DTO to Entity
        UserEntity entity = userMapper.toEntity(request, role);

        // Hash password before saving to DB
        entity.setUserPassword(passwordEncoder.encode(request.getUserPassword()));

        UserEntity savedEntity = userRepo.save(entity);
        return userMapper.toRespond(savedEntity);
    }

    // 2. Get User DTO by ID
    @Transactional(readOnly = true)
    public UserRespond getUserById(String userId) {
        UserEntity entity = getUserEntityById(userId);
        return userMapper.toRespond(entity);
    }

    // 3. Get User JPA Entity by ID (For Service-to-Service lookups)
    @Transactional(readOnly = true)
    public UserEntity getUserEntityById(String userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }

    // 4. List all users
    @Transactional(readOnly = true)
    public List<UserRespond> getAllUsers() {
        return userRepo.findAll().stream()
                .map(userMapper::toRespond)
                .collect(Collectors.toList());
    }

    // 5. Update user profile details
    @Transactional
    public UserRespond updateUser(String userId, UserRequest request) {
        UserEntity existingUser = getUserEntityById(userId);

        existingUser.setUserFname(request.getUserFname());
        existingUser.setUserLname(request.getUserLname());
        existingUser.setUserEmail(request.getUserEmail());

        if (request.getRoleId() != null) {
            RoleEntity role = roleService.getRoleEntityById(request.getRoleId());
            existingUser.setRole(role);
        }

        UserEntity updatedUser = userRepo.save(existingUser);
        return userMapper.toRespond(updatedUser);
    }

    // 6. Change Password (Requires matching old password)
    @Transactional
    public void changePassword(String userId, String oldPassword, String newPassword) {
        UserEntity user = getUserEntityById(userId);

        // Verify old password against stored BCrypt hash
        if (!passwordEncoder.matches(oldPassword, user.getUserPassword())) {
            throw new IllegalArgumentException("Incorrect old password provided!");
        }

        user.setUserPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);
    }

    // 7. Toggle Enabled Status (Soft Deactivation / Reactivation)
    @Transactional
    public UserRespond toggleUserEnabled(String userId, Boolean enabled) {
        UserEntity user = getUserEntityById(userId);
        user.setEnabled(enabled);
        UserEntity updatedUser = userRepo.save(user);
        return userMapper.toRespond(updatedUser);
    }

    // 8. Delete user
    @Transactional
    public void deleteUser(String userId) {
        if (!userRepo.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }
        userRepo.deleteById(userId);
    }
}