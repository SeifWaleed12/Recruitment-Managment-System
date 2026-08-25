package com.banquemisr.recruitment.cvparsing.service;

import com.banquemisr.recruitment.cvparsing.extractor.CvDataExtractor;
import com.banquemisr.recruitment.cvparsing.model.ParsedCv;
import com.banquemisr.recruitment.cvparsing.parser.CvParserRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CvParsingServiceTest {

    @Mock
    private CvParserRegistry parserRegistry;

    @Mock
    private CvDataExtractor dataExtractor;

    @InjectMocks
    private CvParsingService cvParsingService;

    // ---------- validation ----------

    @Test
    void parseInMemory_whenFileNull_throwsIllegalArgumentException() {

        assertThatThrownBy(() -> cvParsingService.parseInMemory(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void parseInMemory_whenFileEmpty_throwsIllegalArgumentException() {

        MultipartFile emptyFile = new MockMultipartFile("file", "cv.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> cvParsingService.parseInMemory(emptyFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void parseInMemory_whenFileExceedsMaxSize_throwsIllegalArgumentException() {

        byte[] oversized = new byte[11 * 1024 * 1024];
        MultipartFile bigFile = new MockMultipartFile("file", "cv.pdf", "application/pdf", oversized);

        assertThatThrownBy(() -> cvParsingService.parseInMemory(bigFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10MB");
    }

    @Test
    void parseInMemory_whenUnsupportedFileType_throwsIllegalArgumentException() {

        MultipartFile file = new MockMultipartFile("file", "cv.txt", "text/plain", "content".getBytes());

        when(parserRegistry.isSupported("text/plain", "cv.txt")).thenReturn(false);

        assertThatThrownBy(() -> cvParsingService.parseInMemory(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported file type");
    }

    // ---------- success path ----------

    @Test
    void parseInMemory_whenValidPdf_returnsParsedCv() throws Exception {

        MultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "raw cv bytes".getBytes());
        ParsedCv expected = ParsedCv.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane@example.com")
                .skills(Set.of("Java"))
                .build();

        when(parserRegistry.isSupported("application/pdf", "cv.pdf")).thenReturn(true);
        when(parserRegistry.parseToText(any(InputStream.class), eq("application/pdf"), eq("cv.pdf")))
                .thenReturn("extracted raw text");
        when(dataExtractor.extract("extracted raw text", "cv.pdf")).thenReturn(expected);

        ParsedCv result = cvParsingService.parseInMemory(file);

        assertThat(result.getFirstName()).isEqualTo("Jane");
        assertThat(result.getEmail()).isEqualTo("jane@example.com");
        verify(parserRegistry).parseToText(any(InputStream.class), eq("application/pdf"), eq("cv.pdf"));
    }

    // ---------- failure path ----------

    @Test
    void parseInMemory_whenParserThrowsUnexpectedException_wrapsInRuntimeException() throws Exception {

        MultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "raw cv bytes".getBytes());

        when(parserRegistry.isSupported("application/pdf", "cv.pdf")).thenReturn(true);
        when(parserRegistry.parseToText(any(InputStream.class), eq("application/pdf"), eq("cv.pdf")))
                .thenThrow(new RuntimeException("Tika failure"));

        assertThatThrownBy(() -> cvParsingService.parseInMemory(file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to extract text from CV");
    }

    @Test
    void parseInMemory_whenReadingInputStreamFails_wrapsInRuntimeException() throws Exception {

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(100L);
        when(file.getOriginalFilename()).thenReturn("cv.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(parserRegistry.isSupported("application/pdf", "cv.pdf")).thenReturn(true);
        when(file.getInputStream()).thenThrow(new IOException("disk error"));

        assertThatThrownBy(() -> cvParsingService.parseInMemory(file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to extract text from CV");
    }
}