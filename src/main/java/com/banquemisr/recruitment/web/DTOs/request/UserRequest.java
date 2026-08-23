package com.banquemisr.recruitment.web.DTOs.request;

import com.banquemisr.recruitment.data.enums.Role;
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

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String userEmail;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String userPassword;

    @NotBlank(message = "First name is required")
    private String userFname;

    @NotBlank(message = "Last name is required")
    private String userLname;

    private Role role;

    @Builder.Default
    private Boolean enabled = true;
}
