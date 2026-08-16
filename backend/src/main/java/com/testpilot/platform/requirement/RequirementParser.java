package com.testpilot.platform.requirement;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

@Component
public class RequirementParser {

    public String parse(String fileType, InputStream inputStream) throws IOException {
        return switch (fileType.toUpperCase()) {
            case "TXT", "MD", "MARKDOWN" -> new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );
            case "PDF" -> parsePdf(inputStream.readAllBytes());
            case "DOCX" -> parseDocx(inputStream);
            default -> throw new IOException("Unsupported requirement file type: " + fileType);
        };
    }

    private String parsePdf(byte[] bytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(document).trim();
        }
    }

    private String parseDocx(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder content = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                appendLine(content, paragraph.getText());
            }
            document.getTables().forEach(table -> table.getRows().forEach(row -> {
                row.getTableCells().forEach(cell -> appendLine(content, cell.getText()));
            }));
            return content.toString().trim();
        }
    }

    private void appendLine(StringBuilder target, String value) {
        if (value != null && !value.isBlank()) {
            if (target.length() > 0) {
                target.append('\n');
            }
            target.append(value.trim());
        }
    }
}
