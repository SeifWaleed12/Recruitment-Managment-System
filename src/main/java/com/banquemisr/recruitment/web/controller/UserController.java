package com.banquemisr.recruitment.web.controller;

import com.banquemisr.recruitment.data.enums.Role;
import com.banquemisr.recruitment.service.UserService;
import com.banquemisr.recruitment.web.DTOs.request.UserRequest;
import com.banquemisr.recruitment.web.DTOs.respond.UserRespond;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserRespond> getAllUsers() {
        return this.userService.getAllUsers();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserRespond getUserById(@PathVariable(name = "id") String userId) {
        return this.userService.getUserById(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UserRespond createUser(@Valid @RequestBody UserRequest userRequest) {
        return this.userService.createUser(userRequest);
    }

    @PostMapping("/interviewer")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public UserRespond createInterviewer(@Valid @RequestBody UserRequest userRequest) {
        return this.userService.createInterviewer(userRequest);
    }

    @PostMapping("/hr")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public UserRespond createHr(@Valid @RequestBody UserRequest userRequest) {
        return this.userService.createHr(userRequest);
    }

    @PostMapping("/admin")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('HR')")
    public UserRespond createAdmin(@Valid @RequestBody UserRequest userRequest) {
        return this.userService.createAdmin(userRequest);
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserRespond changeUserRole(
            @PathVariable(name = "id") String userId,
            @RequestParam(name = "role") Role newRole) {
        return this.userService.changeUserRole(userId, newRole);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserRespond updateUser(
            @PathVariable(name = "id") String userId,
            @Valid @RequestBody UserRequest userRequest) {
        return this.userService.updateUser(userId, userRequest);
    }

    @PatchMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void changePassword(
            @PathVariable(name = "id") String userId,
            @RequestParam(name = "oldPassword") String oldPassword,
            @RequestParam(name = "newPassword") String newPassword) {
        this.userService.changePassword(userId, oldPassword, newPassword);
    }

    @PatchMapping("/{id}/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public UserRespond toggleUserEnabled(
            @PathVariable(name = "id") String userId,
            @RequestParam(name = "enabled") Boolean enabled) {
        return this.userService.toggleUserEnabled(userId, enabled);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(@PathVariable(name = "id") String userId) {
        this.userService.deleteUser(userId);
    }
}
