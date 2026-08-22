package com.banquemisr.recruitment.cvparsing.extractor;

import com.banquemisr.recruitment.cvparsing.model.ParsedCv;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class CvDataExtractor {

    private final EmailExtractor emailExtractor;
    private final PhoneExtractor phoneExtractor;
    private final NameExtractor nameExtractor;
    private final ExperienceExtractor experienceExtractor;
    private final SkillsExtractor skillsExtractor;

    /**
     * Extracts structured fields (name, email, phone, years of experience, skills) from raw CV text.
     */
    public ParsedCv extract(String rawText, String originalFilename) {
        NameExtractor.ExtractedName name = nameExtractor.extractName(rawText, originalFilename);
        String email = emailExtractor.extractEmail(rawText)
                .orElse(generateFallbackEmail(name.firstName(), name.lastName()));
        String phone = phoneExtractor.extractPhone(rawText).orElse(null);
        int yearsOfExperience = experienceExtractor.extractYearsOfExperience(rawText);
        Set<String> skills = skillsExtractor.extractSkills(rawText);

        return ParsedCv.builder()
                .firstName(name.firstName())
                .lastName(name.lastName())
                .email(email)
                .phone(phone)
                .yearsOfExperience(yearsOfExperience)
                .skills(skills)
                .rawText(rawText)
                .build();
    }

    private String generateFallbackEmail(String firstName, String lastName) {
        String cleanFirst = firstName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        String cleanLast = lastName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        return cleanFirst + "." + cleanLast + "." + System.currentTimeMillis() + "@parsed-candidate.com";
    }
}
