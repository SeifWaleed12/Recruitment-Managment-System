package com.banquemisr.recruitment.cvparsing;

import com.banquemisr.recruitment.cvparsing.parser.CvParserRegistry;
import com.banquemisr.recruitment.cvparsing.parser.DocxCvParser;
import com.banquemisr.recruitment.cvparsing.parser.PdfCvParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CvParserStrategyTest {

    private PdfCvParser pdfCvParser;
    private DocxCvParser docxCvParser;
    private CvParserRegistry registry;

    @BeforeEach
    void setUp() {
        pdfCvParser = new PdfCvParser();
        docxCvParser = new DocxCvParser();
        registry = new CvParserRegistry(List.of(pdfCvParser, docxCvParser));
    }

    @Test
    @DisplayName("PdfCvParser should support PDF mime types and extensions")
    void testPdfParserSupport() {
        assertTrue(pdfCvParser.supports("application/pdf", "resume.pdf"));
        assertTrue(pdfCvParser.supports(null, "candidate_cv.PDF"));
        assertFalse(pdfCvParser.supports("application/msword", "resume.doc"));
    }

    @Test
    @DisplayName("DocxCvParser should support DOCX and DOC mime types and extensions")
    void testDocxParserSupport() {
        assertTrue(docxCvParser.supports("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "cv.docx"));
        assertTrue(docxCvParser.supports("application/msword", "cv.doc"));
        assertTrue(docxCvParser.supports(null, "cv.DOCX"));
        assertFalse(docxCvParser.supports("application/pdf", "cv.pdf"));
    }

    @Test
    @DisplayName("CvParserRegistry should validate supported file formats")
    void testRegistrySupportedFormats() {
        assertTrue(registry.isSupported("application/pdf", "cv.pdf"));
        assertTrue(registry.isSupported("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "cv.docx"));
        assertFalse(registry.isSupported("image/png", "photo.png"));
        assertFalse(registry.isSupported("application/x-executable", "file.exe"));
    }
}
