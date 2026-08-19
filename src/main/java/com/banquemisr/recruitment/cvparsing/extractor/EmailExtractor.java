package com.banquemisr.recruitment.cvparsing.extractor;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class EmailExtractor {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}"
    );

    public Optional<String> extractEmail(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = EMAIL_PATTERN.matcher(text);
        if (matcher.find()) {
            return Optional.of(matcher.group().trim().toLowerCase());
        }
        return Optional.empty();
    }
}
