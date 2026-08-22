package com.banquemisr.recruitment.cvparsing.extractor;

import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ExperienceExtractor {

    // Pattern 1: Direct statements like "5+ years of experience", "3 yrs exp", "over 4 years"
    private static final Pattern DIRECT_EXP_PATTERN = Pattern.compile(
            "(?i)(?:(?:over|more than|approx(?:imately)?|around)\\s+)?(\\d{1,2})\\+?\\s*(?:years?|yrs?)(?:\\s+(?:of\\s+)?(?:experience|exp|in\\s+software|in\\s+banking|in\\s+development|in\\s+engineering))?"
    );

    private static final Pattern LABELED_EXP_PATTERN = Pattern.compile(
            "(?i)(?:experience|total\\s+experience)\\s*[:\\-]\\s*(\\d{1,2})\\+?\\s*(?:years?|yrs?)"
    );

    // Pattern 2: Date intervals like "2018 - 2023", "2019 - Present", "2020 – Now"
    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile(
            "\\b(19\\d{2}|20\\d{2})\\s*(?:-|–|—|to)\\s*(19\\d{2}|20\\d{2}|present|current|now)\\b",
            Pattern.CASE_INSENSITIVE
    );

    public int extractYearsOfExperience(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        // 1. Check labeled patterns first (e.g. "Experience: 5 years")
        Matcher labeledMatcher = LABELED_EXP_PATTERN.matcher(text);
        if (labeledMatcher.find()) {
            try {
                int yoe = Integer.parseInt(labeledMatcher.group(1));
                if (yoe >= 0 && yoe <= 45) return yoe;
            } catch (NumberFormatException ignored) {}
        }

        // 2. Check direct mentions in summary/intro
        Matcher directMatcher = DIRECT_EXP_PATTERN.matcher(text);
        while (directMatcher.find()) {
            try {
                int yoe = Integer.parseInt(directMatcher.group(1));
                if (yoe > 0 && yoe <= 45) return yoe;
            } catch (NumberFormatException ignored) {}
        }

        // 3. Fallback: Estimate from date ranges (e.g. 2018 - 2024 -> 6 years)
        int currentYear = Year.now().getValue();
        Matcher dateMatcher = DATE_RANGE_PATTERN.matcher(text);
        int earliestYear = Integer.MAX_VALUE;
        int latestYear = 0;
        int intervalCount = 0;

        while (dateMatcher.find()) {
            try {
                int start = Integer.parseInt(dateMatcher.group(1));
                String endStr = dateMatcher.group(2).toLowerCase();
                int end = (endStr.equals("present") || endStr.equals("current") || endStr.equals("now"))
                        ? currentYear
                        : Integer.parseInt(endStr);

                if (start >= 1980 && start <= currentYear && end >= start && end <= currentYear) {
                    earliestYear = Math.min(earliestYear, start);
                    latestYear = Math.max(latestYear, end);
                    intervalCount++;
                }
            } catch (Exception ignored) {}
        }

        if (intervalCount > 0 && earliestYear != Integer.MAX_VALUE && latestYear >= earliestYear) {
            int calculatedSpan = latestYear - earliestYear;
            if (calculatedSpan >= 0 && calculatedSpan <= 45) {
                return calculatedSpan;
            }
        }

        return 0;
    }
}
