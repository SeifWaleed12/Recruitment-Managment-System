package com.banquemisr.recruitment.cvparsing;

import com.banquemisr.recruitment.cvparsing.extractor.*;
import com.banquemisr.recruitment.cvparsing.model.ParsedCv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CvExtractorsTest {

    private EmailExtractor emailExtractor;
    private PhoneExtractor phoneExtractor;
    private NameExtractor nameExtractor;
    private ExperienceExtractor experienceExtractor;
    private SkillsExtractor skillsExtractor;
    private CvDataExtractor cvDataExtractor;

    @BeforeEach
    void setUp() {
        emailExtractor = new EmailExtractor();
        phoneExtractor = new PhoneExtractor();
        nameExtractor = new NameExtractor();
        experienceExtractor = new ExperienceExtractor();
        skillsExtractor = new SkillsExtractor();
        cvDataExtractor = new CvDataExtractor(
                emailExtractor,
                phoneExtractor,
                nameExtractor,
                experienceExtractor,
                skillsExtractor
        );
    }

    @Test
    @DisplayName("Should extract valid email from CV text")
    void testExtractEmail() {
        String cvText = """
                Ahmed Mohamed
                Software Engineer
                Email: ahmed.mohamed@banquemisr.com
                Phone: +201012345678
                """;

        Optional<String> email = emailExtractor.extractEmail(cvText);
        assertTrue(email.isPresent());
        assertEquals("ahmed.mohamed@banquemisr.com", email.get());
    }

    @Test
    @DisplayName("Should extract Egyptian and international phone numbers")
    void testExtractPhone() {
        String cvText1 = "Contact: +20 100 123 4567";
        String cvText2 = "Mobile: 01112345678";

        Optional<String> phone1 = phoneExtractor.extractPhone(cvText1);
        Optional<String> phone2 = phoneExtractor.extractPhone(cvText2);

        assertTrue(phone1.isPresent());
        assertTrue(phone2.isPresent());
    }

    @Test
    @DisplayName("Should extract candidate name from header or explicit label")
    void testExtractName() {
        String cvText = """
                Mahmoud Ali
                Senior Backend Developer
                mahmoud.ali@example.com
                """;

        NameExtractor.ExtractedName name = nameExtractor.extractName(cvText, "Mahmoud_Ali_CV.pdf");
        assertEquals("Mahmoud", name.firstName());
        assertEquals("Ali", name.lastName());
    }

    @Test
    @DisplayName("Should extract years of experience from direct phrases and date intervals")
    void testExtractExperience() {
        String cvTextDirect = "Senior Java Engineer with 6+ years of experience in enterprise banking systems.";
        int yoeDirect = experienceExtractor.extractYearsOfExperience(cvTextDirect);
        assertEquals(6, yoeDirect);

        String cvTextInterval = """
                Work Experience:
                Software Developer | Bank Misr (2019 - 2024)
                Junior Developer | Tech Corp (2018 - 2019)
                """;
        int yoeInterval = experienceExtractor.extractYearsOfExperience(cvTextInterval);
        assertTrue(yoeInterval >= 5);
    }

    @Test
    @DisplayName("Should extract multiple controlled technical skills")
    void testExtractSkills() {
        String cvText = """
                Technical Skills:
                - Programming: Java, Python, TypeScript, SQL
                - Frameworks: Spring Boot, Hibernate, React, Node.js
                - Cloud & DevOps: Docker, Kubernetes, AWS, CI/CD, Git
                - Architecture: Microservices, REST API, Kafka, PostgreSQL
                """;

        Set<String> skills = skillsExtractor.extractSkills(cvText);
        assertTrue(skills.contains("Java"));
        assertTrue(skills.contains("Spring Boot"));
        assertTrue(skills.contains("Docker"));
        assertTrue(skills.contains("Kubernetes"));
        assertTrue(skills.contains("PostgreSQL"));
        assertTrue(skills.contains("React"));
        assertTrue(skills.contains("Microservices"));
        assertTrue(skills.contains("Kafka"));
    }

    @Test
    @DisplayName("Should assemble complete ParsedCv model via coordinator")
    void testCvDataExtractorComplete() {
        String cvText = """
                Sara Hassan
                sara.hassan@example.com | +20 122 345 6789
                Summary:
                Full Stack Developer with 4 years of experience building scalable financial software.
                Skills:
                Java, Spring Boot, PostgreSQL, Docker, Angular, REST API, Git
                """;

        ParsedCv parsed = cvDataExtractor.extract(cvText, "Sara_Hassan_Resume.pdf");

        assertEquals("Sara", parsed.getFirstName());
        assertEquals("Hassan", parsed.getLastName());
        assertEquals("sara.hassan@example.com", parsed.getEmail());
        assertNotNull(parsed.getPhone());
        assertEquals(4, parsed.getYearsOfExperience());
        assertTrue(parsed.getSkills().contains("Java"));
        assertTrue(parsed.getSkills().contains("Spring Boot"));
        assertTrue(parsed.getSkills().contains("Docker"));
        assertTrue(parsed.getSkills().contains("Angular"));
    }
}
