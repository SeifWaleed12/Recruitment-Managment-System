package com.banquemisr.recruitment.Authentication.Security.Property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "Ldap")
public class LdapProperties {

    private String urls;
    private String base;
    private String username;
    private String password;
}