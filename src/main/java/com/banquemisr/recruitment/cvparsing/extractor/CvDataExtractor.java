package com.banquemisr.recruitment.cvparsing.extractor;

import com.banquemisr.recruitment.cvparsing.llm.LlmClient;
import com.banquemisr.recruitment.cvparsing.model.ParsedCv;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;


@Component
@RequiredArgsConstructor
@Slf4j
public class CvDataExtractor {

    private static final int MAX_TEXT_CHARS = 15000;

    private static final String SYSTEM_PROMPT = """
            You are a resume/CV parsing engine. You will be given the raw text extracted from a CV.
            Extract the candidate's structured information and respond with STRICT JSON ONLY -
            no markdown, no code fences, no explanation - matching exactly this schema:
            {
              "firstName": string,
              "lastName": string,
              "email": string or null,
              "phone": string or null,
              "yearsOfExperience": integer (total years of professional experience, 0 if unknown),
              "skills": array of strings (canonical technical skill/technology names, deduplicated)
            }
            If a field cannot be determined, use null (or 0 for yearsOfExperience, or [] for skills).
            Respond with the JSON object only.
            """;

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ParsedCv extract(String rawText, String originalFilename) {
        String safeText = truncate(rawText);

        try {
            String userPrompt = "Filename: " + (originalFilename != null ? originalFilename : "unknown")
                    + "\n\nCV TEXT:\n" + safeText;

            String llmResponse = llmClient.complete(SYSTEM_PROMPT, userPrompt);
            ParsedCv parsedCv = parseJsonResponse(llmResponse, rawText);

            log.info("LLM parsed CV for candidate '{} {}' with {} skills identified",
                    parsedCv.getFirstName(), parsedCv.getLastName(), parsedCv.getSkills().size());

            return parsedCv;
        } catch (Exception ex) {
            log.error("LLM-based CV extraction failed for '{}': {}", originalFilename, ex.getMessage(), ex);
            throw new RuntimeException("Failed to extract structured data from CV via LLM: " + ex.getMessage(), ex);
        }
    }

    private ParsedCv parseJsonResponse(String llmResponse, String rawText) throws Exception {
        JsonNode node = objectMapper.readTree(stripToJson(llmResponse));

        String firstName = textOrDefault(node.get("firstName"), "Unknown");
        String lastName = textOrDefault(node.get("lastName"), "Candidate");
        String email = textOrDefault(node.get("email"), null);
        String phone = textOrDefault(node.get("phone"), null);
        int yearsOfExperience = node.hasNonNull("yearsOfExperience") ? node.get("yearsOfExperience").asInt(0) : 0;

        Set<String> skills = new LinkedHashSet<>();
        if (node.has("skills") && node.get("skills").isArray()) {
            node.get("skills").forEach(s -> {
                if (s != null && !s.isNull() && !s.asText().isBlank()) {
                    skills.add(s.asText().trim());
                }
            });
        }

        if (email == null || email.isBlank()) {
            email = generateFallbackEmail(firstName, lastName);
        }

        return ParsedCv.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phone(phone)
                .yearsOfExperience(yearsOfExperience)
                .skills(skills)
                .rawText(rawText)
                .build();
    }

    private String stripToJson(String text) {
        if (text == null) return "{}";
        String trimmed = text.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private String textOrDefault(JsonNode node, String defaultValue) {
        if (node == null || node.isNull() || node.asText().isBlank()) {
            return defaultValue;
        }
        return node.asText().trim();
    }

    private String truncate(String text) {
        if (text == null) return "";
        return text.length() > MAX_TEXT_CHARS ? text.substring(0, MAX_TEXT_CHARS) : text;
    }

    private String generateFallbackEmail(String firstName, String lastName) {
        String cleanFirst = firstName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        String cleanLast = lastName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        return cleanFirst + "." + cleanLast + "." + System.currentTimeMillis() + "@parsed-candidate.com";
    }
}