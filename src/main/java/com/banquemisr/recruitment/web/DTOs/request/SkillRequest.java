package com.banquemisr.recruitment.web.DTOs.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillRequest {
    @NotBlank(message = "Skill name required")
    private String name;
}
