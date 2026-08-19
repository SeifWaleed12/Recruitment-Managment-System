package com.banquemisr.recruitment.web.DTOs.request;

import com.banquemisr.recruitment.data.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransitionStatusRequest {

    @NotNull(message = "New status is required")
    private ApplicationStatus newStatus;
}
