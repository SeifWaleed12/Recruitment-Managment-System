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

    private String apiUrl = "https://generativelanguage.googleapis.com/v1beta";
    private String apiKey;
    private String model = "gemini-2.5-flash";
    private int timeoutMs = 30000;
    private String apiVersion = "v1beta";
    private int maxOutputTokens = 4096;
}