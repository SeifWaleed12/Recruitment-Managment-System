package com.banquemisr.recruitment.web.DTOs.respond;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRespond {

    private String userId;
    private String userEmail;
    private String userFname;
    private String userLname;
    private String roleId;
    private String roleName;
    private Boolean enabled;


}
