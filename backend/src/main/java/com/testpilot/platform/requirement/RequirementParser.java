package com.testpilot.platform.requirement;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.springframework.stereotype.Component;

@Component
public class RequirementParser {

    public String parse(String fileType, InputStream inputStream) throws IOException {
        return switch (fileType.toUpperCase()) {
            case "TXT", "MD", "MARKDOWN", "CSV", "TSV", "JSON", "XML", "HTML", "HTM",
                    "YAML", "YML", "SQL", "LOG", "CONF", "INI", "PROPERTIES", "JAVA", "JS", "TS", "CSS" -> new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );
            case "PDF" -> parsePdf(inputStream.readAllBytes());
            case "DOCX" -> parseDocx(inputStream);
            case "DOC" -> parseDoc(inputStream);
            case "XLS", "XLSX" -> parseWorkbook(inputStream);
            case "PPTX" -> parsePptx(inputStream);
            case "PPT" -> parsePpt(inputStream);
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

    private String parseDoc(InputStream inputStream) throws IOException {
        try (HWPFDocument document = new HWPFDocument(inputStream)) {
            Range range = document.getRange();
            return range.text().trim();
        }
    }

    private String parseWorkbook(InputStream inputStream) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            DataFormatter formatter = new DataFormatter();
            StringBuilder content = new StringBuilder();
            workbook.forEach(sheet -> sheet.forEach(row -> {
                StringBuilder line = new StringBuilder();
                row.forEach(cell -> appendCell(line, formatter, cell));
                appendLine(content, line.toString());
            }));
            return content.toString().trim();
        }
    }

    private String parsePptx(InputStream inputStream) throws IOException {
        try (XMLSlideShow slideshow = new XMLSlideShow(inputStream)) {
            StringBuilder content = new StringBuilder();
            for (XSLFSlide slide : slideshow.getSlides()) {
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        appendLine(content, textShape.getText());
                    }
                }
            }
            return content.toString().trim();
        }
    }

    private String parsePpt(InputStream inputStream) throws IOException {
        try (HSLFSlideShow slideshow = new HSLFSlideShow(inputStream)) {
            StringBuilder content = new StringBuilder();
            for (HSLFSlide slide : slideshow.getSlides()) {
                for (HSLFShape shape : slide.getShapes()) {
                    if (shape instanceof HSLFTextShape textShape) {
                        appendLine(content, textShape.getText());
                    }
                }
            }
            return content.toString().trim();
        }
    }

    private void appendCell(StringBuilder line, DataFormatter formatter, Cell cell) {
        if (line.length() > 0) {
            line.append("\t");
        }
        line.append(formatter.formatCellValue(cell));
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
