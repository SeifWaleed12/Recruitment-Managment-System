package com.banquemisr.recruitment.web.DTOs.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleRequest {

    @NotBlank(message = "Role name is required")
    @Size(max = 50, message = "Role name must not exceed 50 characters")
    private String roleName;

}
