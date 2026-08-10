package com.banquemisr.recruitment.web.DTOs.respond;

import com.banquemisr.recruitment.data.entity.CandidateEntity;
import com.banquemisr.recruitment.data.entity.JobEntity;
import com.banquemisr.recruitment.data.entity.UserEntity;
import com.banquemisr.recruitment.data.enums.ApplicationStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationRespond {

    private String applicationId;
    private String candidateId;
    private String candidateFname;
    private String jobId;
    private String jobTitle;
    private ApplicationStatus status;
    private String assignedRecruiter;

}
