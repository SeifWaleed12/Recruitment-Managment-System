package com.banquemisr.recruitment.web.DTOs.request;


import com.banquemisr.recruitment.data.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateRequest {

    @NotBlank(message = "First name is required")
    private String firstName;
    @NotBlank(message = "Last name is required")
    private String lastName;
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;
    private String phone;
    @Min(value = 0, message = "Years of experience cannot be negative")
    @Builder.Default
    private Integer yearsOfExperience = 0;
    private String createdByUserId;

}
