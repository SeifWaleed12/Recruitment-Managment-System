package com.banquemisr.recruitment.cvparsing;

import com.banquemisr.recruitment.cvparsing.extractor.CvDataExtractor;
import com.banquemisr.recruitment.cvparsing.model.ParsedCv;
import com.banquemisr.recruitment.cvparsing.parser.CvParserRegistry;
import com.banquemisr.recruitment.cvparsing.service.CvParsingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CvParsingServiceTest {

    @Mock
    private CvParserRegistry parserRegistry;

    @Mock
    private CvDataExtractor dataExtractor;

    @InjectMocks
    private CvParsingService cvParsingService;

    @Test
    @DisplayName("parseInMemory - throws IllegalArgumentException when file is null or empty")
    void parseInMemory_EmptyFile_ThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "cv.pdf", "application/pdf", new byte[0]
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cvParsingService.parseInMemory(emptyFile)
        );

        assertThat(exception.getMessage()).contains("Uploaded CV file cannot be empty");
    }

    @Test
    @DisplayName("parseInMemory - throws IllegalArgumentException when file type is unsupported")
    void parseInMemory_UnsupportedFileType_ThrowsException() {
        MockMultipartFile txtFile = new MockMultipartFile(
                "file", "cv.txt", "text/plain", "Some text".getBytes()
        );

        when(parserRegistry.isSupported("text/plain", "cv.txt")).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cvParsingService.parseInMemory(txtFile)
        );

        assertThat(exception.getMessage()).contains("Unsupported file type");
    }

    @Test
    @DisplayName("parseInMemory - extracts data successfully for valid PDF file")
    void parseInMemory_ValidPdf_ExtractsCandidateData() throws Exception {
        byte[] content = "CV Content: Youssef Nabil, Java Developer".getBytes();
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file", "youssef_cv.pdf", "application/pdf", content
        );

        ParsedCv expectedParsedCv = ParsedCv.builder()
                .firstName("Youssef")
                .lastName("Nabil")
                .email("youssef@example.com")
                .skills(Set.of("Java", "Spring Boot"))
                .yearsOfExperience(3)
                .build();

        when(parserRegistry.isSupported("application/pdf", "youssef_cv.pdf")).thenReturn(true);
        when(parserRegistry.parseToText(any(InputStream.class), eq("application/pdf"), eq("youssef_cv.pdf")))
                .thenReturn("CV Content: Youssef Nabil, Java Developer");
        when(dataExtractor.extract("CV Content: Youssef Nabil, Java Developer", "youssef_cv.pdf"))
                .thenReturn(expectedParsedCv);

        ParsedCv result = cvParsingService.parseInMemory(pdfFile);

        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("Youssef");
        assertThat(result.getLastName()).isEqualTo("Nabil");
        assertThat(result.getSkills()).contains("Java", "Spring Boot");
    }
}
