package com.banquemisr.recruitment.cvparsing.parser;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class PdfCvParser implements CvParser {

    @Override
    public boolean supports(String contentType, String filename) {
        if (contentType != null && contentType.equalsIgnoreCase("application/pdf")) {
            return true;
        }
        return filename != null && filename.toLowerCase().endsWith(".pdf");
    }

    @Override
    public String extractText(InputStream inputStream) throws Exception {
        BodyContentHandler handler = new BodyContentHandler(-1); // -1 allows unlimited document length
        Metadata metadata = new Metadata();
        ParseContext parseContext = new ParseContext();
        PDFParser parser = new PDFParser();

        parser.parse(inputStream, handler, metadata, parseContext);
        return handler.toString();
    }
}
