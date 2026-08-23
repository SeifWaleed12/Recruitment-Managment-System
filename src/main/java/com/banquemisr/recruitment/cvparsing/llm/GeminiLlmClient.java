package com.banquemisr.recruitment.cvparsing.llm;

import com.banquemisr.recruitment.cvparsing.config.LlmProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class GeminiLlmClient implements LlmClient {

    private final LlmProperties llmProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiLlmClient(LlmProperties llmProperties) {
        this.llmProperties = llmProperties;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(llmProperties.getTimeoutMs());
        requestFactory.setReadTimeout(llmProperties.getTimeoutMs());

        this.restClient = RestClient.builder()
                .baseUrl(llmProperties.getApiUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        if (llmProperties.getApiKey() == null || llmProperties.getApiKey().isBlank()) {
            throw new IllegalStateException("LLM API key is not configured (app.llm.api-key)");
        }

        Map<String, Object> requestBody = Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", userPrompt))
                )),
                "generationConfig", Map.of("maxOutputTokens", llmProperties.getMaxOutputTokens())
        );

        String path = "/models/" + llmProperties.getModel() + ":generateContent";

        try {
            String rawResponse = restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-goog-api-key", llmProperties.getApiKey())
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return extractText(rawResponse);
        } catch (Exception ex) {
            log.error("LLM API call failed: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Failed to call external LLM API: " + ex.getMessage(), ex);
        }
    }

    private String extractText(String rawResponse) throws Exception {
        JsonNode root = objectMapper.readTree(rawResponse);
        JsonNode candidates = root.path("candidates");

        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new IllegalStateException("LLM API returned no candidates: " + rawResponse);
        }

        JsonNode parts = candidates.get(0).path("content").path("parts");
        StringBuilder text = new StringBuilder();
        if (parts.isArray()) {
            for (JsonNode part : parts) {
                if (part.has("text")) {
                    text.append(part.get("text").asText());
                }
            }
        }

        if (text.isEmpty()) {
            throw new IllegalStateException("LLM API returned no usable text content: " + rawResponse);
        }

        return text.toString();
    }
}