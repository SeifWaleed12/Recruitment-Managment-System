package com.banquemisr.recruitment.web.DTOs.respond;


import com.banquemisr.recruitment.data.enums.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleRespond {

    private Role role;
    private String roleName;

}
