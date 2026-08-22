package com.banquemisr.recruitment.web.controller;

import com.banquemisr.recruitment.service.RoleService;
import com.banquemisr.recruitment.web.DTOs.respond.RoleRespond;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public List<RoleRespond> getAllRoles() {
        return this.roleService.getAllRoles();
    }

    @GetMapping("/{name}")
    public RoleRespond getRoleByName(@PathVariable(name = "name") String name) {
        return this.roleService.getRoleByName(name);
    }
}
