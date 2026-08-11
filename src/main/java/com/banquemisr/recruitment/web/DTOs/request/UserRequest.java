package com.banquemisr.recruitment.web.DTOs.request;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequest {

    @NotBlank(message = "email required")
    @Email(message = "Email must be a valid email address")
    private String userEmail;
    @NotBlank(message = "password required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String userPassword;
    @NotBlank(message = "First name required")
    private String userFname;
    @NotBlank(message = "Last name required")
    private String userLname;
    @NotBlank(message = "role required")
    private String roleId;
    @Builder.Default
    private Boolean enabled = true;

}
