package com.banquemisr.recruitment.web.DTOs.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagRequest {
    @NotBlank(message = "Tag name is required")
    private String name;
}
