package com.banquemisr.recruitment.web.DTOs.respond;

import com.banquemisr.recruitment.data.enums.Role;
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
    private Role role;
    private String roleName;
    private Boolean enabled;
}
