package com.banquemisr.recruitment.cvparsing.service;

import com.banquemisr.recruitment.cvparsing.extractor.CvDataExtractor;
import com.banquemisr.recruitment.cvparsing.model.ParsedCv;
import com.banquemisr.recruitment.cvparsing.parser.CvParserRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class CvParsingService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final CvParserRegistry parserRegistry;
    private final CvDataExtractor dataExtractor;

    /**
     * Parses an uploaded CV completely in-memory with zero file retention.
     * Extracts text using the registered strategy for PDF/DOCX and runs heuristic extraction.
     */
    public ParsedCv parseInMemory(MultipartFile file) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();

        log.info("Parsing CV in-memory: filename='{}', contentType='{}', size={} bytes",
                originalFilename, contentType, file.getSize());

        try (InputStream inputStream = file.getInputStream()) {
            String rawText = parserRegistry.parseToText(inputStream, contentType, originalFilename);
            ParsedCv parsedCv = dataExtractor.extract(rawText, originalFilename);

            log.info("Successfully parsed CV for candidate '{} {}' with {} skills identified",
                    parsedCv.getFirstName(), parsedCv.getLastName(), parsedCv.getSkills().size());

            return parsedCv;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse CV '{}': {}", originalFilename, e.getMessage(), e);
            throw new RuntimeException("Failed to extract text from CV (" + originalFilename + "): " + e.getMessage(), e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded CV file cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Uploaded file exceeds the maximum allowed size of 10MB");
        }

        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();

        if (!parserRegistry.isSupported(contentType, filename)) {
            throw new IllegalArgumentException("Unsupported file type. Only PDF and DOCX files are allowed.");
        }
    }
}
