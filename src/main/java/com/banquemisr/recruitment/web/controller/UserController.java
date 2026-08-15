package com.banquemisr.recruitment.web.controller;

import com.banquemisr.recruitment.service.UserService;
import com.banquemisr.recruitment.web.DTOs.request.UserRequest;
import com.banquemisr.recruitment.web.DTOs.respond.UserRespond;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<UserRespond> getAllUsers() {
        return this.userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public UserRespond getUserById(@PathVariable(name = "id") String userId) {
        return this.userService.getUserById(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserRespond createUser(@Valid @RequestBody UserRequest userRequest) {
        return this.userService.createUser(userRequest);
    }

    @PutMapping("/{id}")
    public UserRespond updateUser(
            @PathVariable(name = "id") String userId,
            @Valid @RequestBody UserRequest userRequest) {
        return this.userService.updateUser(userId, userRequest);
    }

    @PatchMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @PathVariable(name = "id") String userId,
            @RequestParam(name = "oldPassword") String oldPassword,
            @RequestParam(name = "newPassword") String newPassword) {
        this.userService.changePassword(userId, oldPassword, newPassword);
    }

    @PatchMapping("/{id}/enabled")
    public UserRespond toggleUserEnabled(
            @PathVariable(name = "id") String userId,
            @RequestParam(name = "enabled") Boolean enabled) {
        return this.userService.toggleUserEnabled(userId, enabled);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable(name = "id") String userId) {
        this.userService.deleteUser(userId);
    }
}
