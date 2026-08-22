package com.banquemisr.recruitment.cvparsing;

import com.banquemisr.recruitment.cvparsing.extractor.CvDataExtractor;
import com.banquemisr.recruitment.cvparsing.llm.LlmClient;
import com.banquemisr.recruitment.cvparsing.model.ParsedCv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * CvDataExtractor now delegates field extraction to an external LLM via LlmClient,
 * instead of the old regex-based Name/Email/Phone/Experience/SkillsExtractor pipeline.
 * These tests mock LlmClient so they run offline/deterministically instead of hitting
 * the real Gemini API.
 */
class CvDataExtractorTest {

    private LlmClient llmClient;
    private CvDataExtractor cvDataExtractor;

    @BeforeEach
    void setUp() {
        llmClient = mock(LlmClient.class);
        cvDataExtractor = new CvDataExtractor(llmClient);
    }

    @Test
    @DisplayName("Should parse a complete, well-formed LLM JSON response into ParsedCv")
    void testCvDataExtractorComplete() {
        String cvText = """
                Sara Hassan
                sara.hassan@example.com | +20 122 345 6789
                Summary:
                Full Stack Developer with 4 years of experience building scalable financial software.
                Skills:
                Java, Spring Boot, PostgreSQL, Docker, Angular, REST API, Git
                """;

        String llmJsonResponse = """
                {
                  "firstName": "Sara",
                  "lastName": "Hassan",
                  "email": "sara.hassan@example.com",
                  "phone": "+20 122 345 6789",
                  "yearsOfExperience": 4,
                  "skills": ["Java", "Spring Boot", "PostgreSQL", "Docker", "Angular", "REST API", "Git"]
                }
                """;

        when(llmClient.complete(anyString(), anyString())).thenReturn(llmJsonResponse);

        ParsedCv parsed = cvDataExtractor.extract(cvText, "Sara_Hassan_Resume.pdf");

        assertEquals("Sara", parsed.getFirstName());
        assertEquals("Hassan", parsed.getLastName());
        assertEquals("sara.hassan@example.com", parsed.getEmail());
        assertEquals("+20 122 345 6789", parsed.getPhone());
        assertEquals(4, parsed.getYearsOfExperience());
        assertTrue(parsed.getSkills().contains("Java"));
        assertTrue(parsed.getSkills().contains("Spring Boot"));
        assertTrue(parsed.getSkills().contains("Docker"));
        assertTrue(parsed.getSkills().contains("Angular"));
        assertEquals(cvText, parsed.getRawText());
    }

    @Test
    @DisplayName("Should still parse JSON if the LLM wraps it in markdown code fences")
    void testStripsMarkdownCodeFences() {
        String cvText = "Mahmoud Ali\nmahmoud.ali@example.com";

        String llmJsonResponse = """
```json
                {
                  "firstName": "Mahmoud",
                  "lastName": "Ali",
                  "email": "mahmoud.ali@example.com",
                  "phone": null,
                  "yearsOfExperience": 0,
                  "skills": []
                }
```
                """;

        when(llmClient.complete(anyString(), anyString())).thenReturn(llmJsonResponse);

        ParsedCv parsed = cvDataExtractor.extract(cvText, "Mahmoud_Ali_CV.pdf");

        assertEquals("Mahmoud", parsed.getFirstName());
        assertEquals("Ali", parsed.getLastName());
        assertEquals("mahmoud.ali@example.com", parsed.getEmail());
        assertNull(parsed.getPhone());
        assertEquals(0, parsed.getYearsOfExperience());
        assertTrue(parsed.getSkills().isEmpty());
    }

    @Test
    @DisplayName("Should generate a fallback email when the LLM omits it")
    void testGeneratesFallbackEmailWhenMissing() {
        String cvText = "Ahmed Mohamed\nSoftware Engineer";

        String llmJsonResponse = """
                {
                  "firstName": "Ahmed",
                  "lastName": "Mohamed",
                  "email": null,
                  "phone": "+201012345678",
                  "yearsOfExperience": 2,
                  "skills": ["Java"]
                }
                """;

        when(llmClient.complete(anyString(), anyString())).thenReturn(llmJsonResponse);

        ParsedCv parsed = cvDataExtractor.extract(cvText, "Ahmed_Mohamed_CV.pdf");

        assertEquals("Ahmed", parsed.getFirstName());
        assertEquals("Mohamed", parsed.getLastName());
        assertNotNull(parsed.getEmail());
        assertTrue(parsed.getEmail().contains("ahmed"));
        assertTrue(parsed.getEmail().contains("mohamed"));
    }

    @Test
    @DisplayName("Should throw when the LLM call itself fails")
    void testThrowsWhenLlmCallFails() {
        when(llmClient.complete(anyString(), anyString()))
                .thenThrow(new IllegalStateException("LLM API key is not configured (app.llm.api-key)"));

        assertThrows(RuntimeException.class, () ->
                cvDataExtractor.extract("some cv text", "broken.pdf"));
    }

    @Test
    @DisplayName("Should throw when the LLM returns unparseable content")
    void testThrowsOnMalformedJson() {
        when(llmClient.complete(anyString(), anyString())).thenReturn("not valid json at all");

        assertThrows(RuntimeException.class, () ->
                cvDataExtractor.extract("some cv text", "broken.pdf"));
    }
}