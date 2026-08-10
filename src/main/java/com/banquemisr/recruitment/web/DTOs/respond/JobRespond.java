package com.banquemisr.recruitment.web.DTOs.respond;


import com.banquemisr.recruitment.data.enums.JobStatus;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JobRespond {

    private String jobId;
    private String title;
    private String description;
    private String department;
    private String location;
    private JobStatus status;
    private String createdByUserId;

}
