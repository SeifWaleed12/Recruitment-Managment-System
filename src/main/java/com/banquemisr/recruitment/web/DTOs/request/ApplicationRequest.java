package com.banquemisr.recruitment.web.DTOs.request;

import com.banquemisr.recruitment.data.enums.ApplicationStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationRequest {

    @NotBlank(message = "Candidate ID is required")
    private String candidateId;
    @NotBlank(message = "Job ID is required")
    private String jobId;
    // Optional status (defaults to APPLIED on creation if null)
    private ApplicationStatus status;
    // Optional assigned recruiter user ID
    private String assignedRecruiterId;

}
