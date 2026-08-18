package com.banquemisr.recruitment.cvparsing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParsedCv {

    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    @Builder.Default
    private Integer yearsOfExperience = 0;
    @Builder.Default
    private Set<String> skills = new HashSet<>();
    private String rawText;
}
