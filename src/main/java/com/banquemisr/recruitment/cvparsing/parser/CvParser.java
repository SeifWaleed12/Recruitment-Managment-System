package com.banquemisr.recruitment.cvparsing.parser;

import java.io.InputStream;

/**
 * Strategy interface for extracting raw textual content from different CV file formats.
 */
public interface CvParser {

    /**
     * Determines whether this parser implementation can handle the given content type or filename.
     */
    boolean supports(String contentType, String filename);

    /**
     * Extracts plain text from the provided file input stream.
     */
    String extractText(InputStream inputStream) throws Exception;
}
