package com.banquemisr.recruitment.data.specification;

import com.banquemisr.recruitment.data.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateSearchCriteria {

    private String name;
    private List<String> skills;
    private List<String> tags;
    private ApplicationStatus status;
    private Integer minExperience;
    private Integer maxExperience;
}
