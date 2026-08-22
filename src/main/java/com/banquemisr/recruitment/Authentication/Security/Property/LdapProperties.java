package com.banquemisr.recruitment.Authentication.Security.Property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ldap")
public class LdapProperties {

    private String urls;
    private String base;
    private String username;
    private String password;
    //code under which entries live
    private String peopleOu = "ou=people";
    /**
     * LDAP filter used to locate a user during authentication. {0} is substituted with the
     * login identifier submitted by the client (we use the user's email/mail attribute).
     */
    private String userSearchFilter = "(mail={0})";
}