package com.banquemisr.recruitment.web.controller;


import com.banquemisr.recruitment.service.RoleService;
import com.banquemisr.recruitment.web.DTOs.request.RoleRequest;
import com.banquemisr.recruitment.web.DTOs.respond.RoleRespond;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;


    @GetMapping
    public List<RoleRespond> getAllRoles(){
        return this.roleService.getAllRoles();
    }

    @GetMapping("/{id}")
    public RoleRespond getRoleById(@PathVariable(name="id") String id){
        return this.roleService.getRoleById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoleRespond createRole(@Valid @RequestBody RoleRequest roleRequest){
            return this.roleService.createRole(roleRequest);
    }


}
