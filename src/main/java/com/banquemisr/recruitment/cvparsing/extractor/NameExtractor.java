package com.banquemisr.recruitment.cvparsing.extractor;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class NameExtractor {

    private static final Pattern EXPLICIT_NAME_PATTERN = Pattern.compile(
            "(?i)(?:full\\s*name|name)\\s*[:\\-]\\s*([A-Za-zÀ-ÿ\\s'.\\-]{2,50})"
    );

    private static final List<String> IGNORED_HEADERS = List.of(
            "curriculum vitae", "resume", "cv", "personal details", "profile",
            "personal profile", "contact", "contact details", "contact information",
            "summary", "professional summary", "career objective", "work experience",
            "education", "skills", "experience", "about me", "page 1"
    );

    public record ExtractedName(String firstName, String lastName) {}

    public ExtractedName extractName(String text, String originalFilename) {
        if (text != null && !text.isBlank()) {
            // Strategy 1: Check for explicit "Name: John Doe" pattern
            Matcher explicitMatcher = EXPLICIT_NAME_PATTERN.matcher(text);
            if (explicitMatcher.find()) {
                String matched = explicitMatcher.group(1).trim();
                ExtractedName split = splitFullName(matched);
                if (split != null) return split;
            }

            // Strategy 2: Look at top lines of the document
            String[] lines = text.split("\\R");
            for (int i = 0; i < Math.min(lines.length, 10); i++) {
                String line = lines[i].trim();
                if (line.isBlank() || line.length() < 3 || line.length() > 50) {
                    continue;
                }

                String lower = line.toLowerCase();
                if (IGNORED_HEADERS.contains(lower) || lower.contains("@") || lower.contains("http") || lower.contains(".com")) {
                    continue;
                }

                // Check if line looks like a valid person name (words with letters and spaces)
                if (line.matches("^[A-Za-zÀ-ÿ][A-Za-zÀ-ÿ\\s'.\\-]{1,49}$")) {
                    String[] words = line.split("\\s+");
                    if (words.length >= 2 && words.length <= 4) {
                        return splitFullName(line);
                    } else if (words.length == 1 && i + 1 < lines.length) {
                        String nextLine = lines[i + 1].trim();
                        if (nextLine.matches("^[A-Za-zÀ-ÿ][A-Za-zÀ-ÿ\\s'.\\-]{1,49}$")) {
                            return new ExtractedName(words[0], nextLine);
                        }
                    }
                }
            }
        }

        // Strategy 3: Fallback to filename (e.g. "Ahmed_Hassan_Resume.pdf")
        if (originalFilename != null && !originalFilename.isBlank()) {
            String cleanName = originalFilename
                    .replaceFirst("(?i)\\.(pdf|docx|doc)$", "")
                    .replaceAll("(?i)(resume|cv|curriculum_vitae|profile|final|updated|v\\d+)", "")
                    .replaceAll("[_\\-+.]", " ")
                    .trim();

            if (!cleanName.isBlank()) {
                ExtractedName fromFilename = splitFullName(cleanName);
                if (fromFilename != null) return fromFilename;
            }
        }

        return new ExtractedName("Unknown", "Candidate");
    }

    private ExtractedName splitFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) return null;
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return new ExtractedName(parts[0], "Candidate");
        }
        String firstName = parts[0];
        String lastName = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
        return new ExtractedName(firstName, lastName);
    }
}
