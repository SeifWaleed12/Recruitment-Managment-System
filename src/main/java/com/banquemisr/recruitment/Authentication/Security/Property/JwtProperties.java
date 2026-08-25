package com.banquemisr.recruitment.Authentication.Security.Property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private long accessTokenExpirationMs = 300000;
    private long refreshTokenExpirationMs = 86400000;
    private long resetTokenExpirationMs = 900000;
}
