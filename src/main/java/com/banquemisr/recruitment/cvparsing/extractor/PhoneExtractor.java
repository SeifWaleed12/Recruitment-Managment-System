package com.banquemisr.recruitment.cvparsing.extractor;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PhoneExtractor {

    // Matches Egyptian numbers (+201..., 010..., 011..., 012..., 015...) and international numbers
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:(?:\\+|00)(\\d{1,3})[\\s.-]?)?\\(?(\\d{2,4})\\)?[\\s.-]?(\\d{3,4})[\\s.-]?(\\d{3,5})"
    );

    public Optional<String> extractPhone(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = PHONE_PATTERN.matcher(text);
        while (matcher.find()) {
            String candidate = matcher.group().trim();
            // Clean up unwanted characters to verify digit count
            String digitsOnly = candidate.replaceAll("[^0-9]", "");
            if (digitsOnly.length() >= 8 && digitsOnly.length() <= 15) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }
}
