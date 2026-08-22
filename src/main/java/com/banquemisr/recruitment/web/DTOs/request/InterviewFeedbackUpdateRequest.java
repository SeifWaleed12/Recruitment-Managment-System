package com.banquemisr.recruitment.web.DTOs.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewFeedbackUpdateRequest {
    @NotNull(message = "Overall score is required")
    @DecimalMin(value = "0.0", message = "Score must be at least 0")
    @DecimalMax(value = "9.99", message = "Score must be at most 9.99")
    private Double overallScore;

    private String comments;
}
