package com.banquemisr.recruitment.cvparsing;

import com.banquemisr.recruitment.cvparsing.parser.PdfCvParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PdfCvParserTest {

    private final PdfCvParser parser = new PdfCvParser();

    @Test
    @DisplayName("supports - correctly identifies PDF content type and file extension")
    void supports_PdfFiles_ReturnsTrue() {
        assertThat(parser.supports("application/pdf", "resume.pdf")).isTrue();
        assertThat(parser.supports(null, "candidate_cv.PDF")).isTrue();
        assertThat(parser.supports("application/pdf", null)).isTrue();
    }

    @Test
    @DisplayName("supports - rejects non-PDF content types and extensions")
    void supports_NonPdfFiles_ReturnsFalse() {
        assertThat(parser.supports("application/msword", "doc.docx")).isFalse();
        assertThat(parser.supports("text/plain", "notes.txt")).isFalse();
        assertThat(parser.supports(null, "image.png")).isFalse();
    }
}
