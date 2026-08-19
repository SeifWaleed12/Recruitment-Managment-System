package com.banquemisr.recruitment.cvparsing.parser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CvParserRegistry {

    private final List<CvParser> parsers;

    /**
     * Resolves the appropriate parser strategy and extracts raw text from the input stream.
     */
    public String parseToText(InputStream inputStream, String contentType, String filename) throws Exception {
        CvParser selectedParser = parsers.stream()
                .filter(parser -> parser.supports(contentType, filename))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported file format: " + filename + " (" + contentType + "). Only PDF and DOCX files are supported."
                ));

        return selectedParser.extractText(inputStream);
    }

    /**
     * Checks whether the file format is supported by any registered parser.
     */
    public boolean isSupported(String contentType, String filename) {
        return parsers.stream().anyMatch(p -> p.supports(contentType, filename));
    }
}
