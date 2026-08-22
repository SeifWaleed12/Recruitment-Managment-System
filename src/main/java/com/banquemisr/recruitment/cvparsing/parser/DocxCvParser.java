package com.banquemisr.recruitment.cvparsing.parser;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Set;

@Component
public class DocxCvParser implements CvParser {

    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword",
            "application/x-tika-ooxml",
            "application/x-tika-msoffice"
    );

    @Override
    public boolean supports(String contentType, String filename) {
        if (contentType != null && SUPPORTED_MIME_TYPES.contains(contentType.toLowerCase())) {
            return true;
        }
        if (filename != null) {
            String lower = filename.toLowerCase();
            return lower.endsWith(".docx") || lower.endsWith(".doc");
        }
        return false;
    }

    @Override
    public String extractText(InputStream inputStream) throws Exception {
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        ParseContext parseContext = new ParseContext();
        AutoDetectParser parser = new AutoDetectParser();

        parser.parse(inputStream, handler, metadata, parseContext);
        return handler.toString();
    }
}
