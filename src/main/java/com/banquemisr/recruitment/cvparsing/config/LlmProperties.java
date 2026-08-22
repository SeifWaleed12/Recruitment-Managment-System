package com.banquemisr.recruitment.cvparsing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.llm")
public class LlmProperties {

    private String apiUrl;
    private String apiKey;
    private String model;
    private int timeoutMs;
    private String apiVersion;
    private int maxOutputTokens;
}