package com.banquemisr.recruitment.cvparsing.llm;


public interface LlmClient {
    String complete(String systemPrompt, String userPrompt);
}