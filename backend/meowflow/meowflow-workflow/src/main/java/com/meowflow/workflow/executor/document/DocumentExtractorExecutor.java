package com.meowflow.workflow.executor.document;

import com.meowflow.common.exception.NodeException;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import com.meowflow.workflow.executor.AbstractNodeExecutor;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DocumentExtractorExecutor extends AbstractNodeExecutor {

    private final RestTemplate restTemplate;

    @Override
    public NodeType getNodeType() {
        return NodeType.DOCUMENT_EXTRACTOR;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        Object sourceObj = input.get("fileUrl");
        if (sourceObj == null) {
            sourceObj = input.get("document");
        }
        if (sourceObj == null && input.get("fileRef") instanceof Map<?, ?> ref) {
            sourceObj = ref.get("path");
        }
        if (sourceObj == null) {
            throw new NodeException(node.getId(), node.getType().getCode(), "文件 URL、路径或 fileRef 必填");
        }

        String source = sourceObj.toString();
        String fileType = input.get("fileType") != null
                ? input.get("fileType").toString().toLowerCase()
                : inferType(source);
        if ("auto".equals(fileType)) {
            fileType = inferType(source);
        }

        try {
            byte[] bytes = readBytes(source);
            ExtractResult extracted = extract(bytes, fileType);
            Map<String, Object> output = new HashMap<>();
            output.put("text", extracted.text());
            output.put("pages", extracted.pages());
            output.put("fileType", fileType);
            output.put("source", source);
            return NodeResult.success(node.getId(), node.getType(), node.getName(), output);
        } catch (Exception e) {
            throw new NodeException(node.getId(), node.getType().getCode(),
                    "文档提取失败: " + e.getMessage(), e);
        }
    }

    private byte[] readBytes(String source) throws Exception {
        if (source.startsWith("http://") || source.startsWith("https://")) {
            return restTemplate.getForObject(source, byte[].class);
        }
        return Files.readAllBytes(new File(source).toPath());
    }

    private String inferType(String source) {
        String lower = source.toLowerCase();
        if (lower.endsWith(".pdf")) return "pdf";
        if (lower.endsWith(".docx")) return "docx";
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) return "xlsx";
        if (lower.endsWith(".md")) return "md";
        return "txt";
    }

    private ExtractResult extract(byte[] bytes, String fileType) throws Exception {
        return switch (fileType) {
            case "pdf" -> extractPdf(bytes);
            case "docx" -> extractDocx(bytes);
            case "xlsx", "xls" -> extractXlsx(bytes);
            default -> new ExtractResult(new String(bytes, StandardCharsets.UTF_8), 0);
        };
    }

    private ExtractResult extractPdf(byte[] bytes) throws Exception {
        try (PDDocument document = PDDocument.load(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return new ExtractResult(stripper.getText(document), document.getNumberOfPages());
        }
    }

    private ExtractResult extractDocx(byte[] bytes) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                if (!paragraph.getText().isBlank()) sb.append(paragraph.getText()).append("\n");
            }
            for (XWPFTable table : document.getTables()) {
                for (var row : table.getRows()) {
                    for (var cell : row.getTableCells()) {
                        sb.append(cell.getText()).append("\t");
                    }
                    sb.append("\n");
                }
            }
        }
        return new ExtractResult(sb.toString(), 0);
    }

    private ExtractResult extractXlsx(byte[] bytes) throws Exception {
        StringBuilder sb = new StringBuilder();
        DataFormatter formatter = new DataFormatter();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                XSSFSheet sheet = workbook.getSheetAt(s);
                sb.append("# ").append(sheet.getSheetName()).append("\n");
                for (var row : sheet) {
                    if (row == null) continue;
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        Cell cell = row.getCell(c);
                        if (cell != null) {
                            sb.append(formatter.formatCellValue(cell)).append("\t");
                        }
                    }
                    sb.append("\n");
                }
            }
        }
        return new ExtractResult(sb.toString(), 0);
    }

    private record ExtractResult(String text, int pages) {
    }
}
