package com.banquemisr.recruitment.web.DTOs.respond;


import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateRespond {

    private String candidateId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private Integer yearsOfExperience;
    private String cvFilePath;
    private String cvOriginalFilename;
    private String cvFileType;
    private String createdByUserId;
    private String createdByUserName;
    private Set<SkillRespond> skills;
    private Set<TagRespond> tags;

}
