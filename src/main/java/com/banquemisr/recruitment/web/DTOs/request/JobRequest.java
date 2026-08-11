package com.banquemisr.recruitment.web.DTOs.request;


import com.banquemisr.recruitment.data.enums.JobStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JobRequest {

    @NotBlank(message = "Job title is required")
    @Size(max = 150, message = "Job title must not exceed 150 characters")
    private String title;
    @NotBlank(message = "Job description is required")
    private String description;
    @NotBlank(message = "Department is required")
    @Size(max = 100, message = "Department must not exceed 100 characters")
    private String department;
    @NotBlank(message = "Location is required")
    @Size(max = 100, message = "Location must not exceed 100 characters")
    private String location;
    @NotNull(message = "Job status is required")
    private JobStatus status;
    @NotBlank(message = "Created by User ID is required")
    private String createdByUserId;

}
