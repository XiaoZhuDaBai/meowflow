package com.meowflow.infra.knowledge;

import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 文档解析器 - 支持多种格式的文档解析
 * <p>
 * 支持格式:
 * <ul>
 *   <li>text/plain, text/markdown - 直接返回</li>
 *   <li>text/html - 去除 HTML 标签</li>
 *   <li>application/json - 去除 JSON 语法字符</li>
 *   <li>application/pdf - Apache Tika PDF 解析</li>
 *   <li>application/vnd.openxmlformats-officedocument.wordprocessingml.document (DOCX) - Apache Tika</li>
 *   <li>application/vnd.ms-excel (XLS) - Apache Tika</li>
 *   <li>application/vnd.openxmlformats-officedocument.spreadsheetml.sheet (XLSX) - Apache Tika</li>
 *   <li>application/vnd.ms-powerpoint (PPT) - Apache Tika</li>
 *   <li>application/vnd.openxmlformats-officedocument.presentationml.presentation (PPTX) - Apache Tika</li>
 *   <li>其他格式 - Tika 自动检测</li>
 * </ul>
 */
@Slf4j
@Component
public class DocumentParser {

    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int DEFAULT_CHUNK_OVERLAP = 50;

    private static final int MAX_TEXT_LENGTH = 10 * 1024 * 1024; // 10MB
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern JSON_NOISE_PATTERN = Pattern.compile("[{}\\[\\]\":]|(:\\s*)|(,\\s*)");

    private final Tika tika;
    private final AutoDetectParser autoParser;

    public DocumentParser() {
        this.tika = new Tika();
        this.autoParser = new AutoDetectParser();
    }

    /**
     * 解析并分块文档内容
     */
    public List<String> parseAndChunk(String content, String contentType) {
        return parseAndChunk(content, contentType, DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);
    }

    public List<String> parseAndChunk(String content, String contentType, int chunkSize, int chunkOverlap) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }

        String text = extractText(content, contentType);
        return splitText(text, chunkSize, chunkOverlap);
    }

    /**
     * 从字符串内容提取纯文本
     */
    public String extractText(String content, String contentType) {
        if (content == null) return "";

        if (contentType == null) {
            return content;
        }

        String ct = contentType.toLowerCase();
        return switch (ct) {
            case "text/plain", "text/markdown", "text/x-markdown" -> content;
            case "text/html", "application/xhtml+xml" -> extractHtmlText(content);
            case "application/json" -> extractJsonText(content);
            default -> {
                // 其他格式尝试 Tika 解析
                if (ct.contains("pdf") || ct.contains("word") || ct.contains("document") ||
                    ct.contains("sheet") || ct.contains("excel") || ct.contains("powerpoint") ||
                    ct.contains("presentation") || ct.contains("rtf") || ct.contains("xml")) {
                    yield extractWithTika(content, ct);
                }
                yield content;
            }
        };
    }

    /**
     * 从二进制流提取纯文本（用于文件上传场景）
     *
     * @param inputStream 文件二进制流
     * @param contentType MIME 类型（可为空，由 Tika 自动检测）
     * @return 提取的纯文本
     */
    public String extractFromInputStream(InputStream inputStream, String contentType) {
        try {
            BodyContentHandler handler = new BodyContentHandler(MAX_TEXT_LENGTH);
            Metadata metadata = new Metadata();
            if (contentType != null && !contentType.isBlank()) {
                metadata.set(Metadata.CONTENT_TYPE, contentType);
            }
            ParseContext context = new ParseContext();
            autoParser.parse(inputStream, handler, metadata, context);
            return handler.toString().trim();
        } catch (TikaException e) {
            log.error("Tika parse error", e);
            throw new BizException(ResultCode.PARAM_ERROR, "文档解析失败: " + e.getMessage());
        } catch (SAXException e) {
            log.error("SAX parse error", e);
            throw new BizException(ResultCode.PARAM_ERROR, "文档解析失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("Extract from input stream error", e);
            throw new BizException(ResultCode.PARAM_ERROR, "文档解析失败: " + e.getMessage());
        }
    }

    /**
     * 从字节数组提取纯文本（用于 MinIO 等二进制存储）
     */
    public String extractFromBytes(byte[] data, String contentType) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data)) {
            return extractFromInputStream(bis, contentType);
        } catch (Exception e) {
            log.error("Extract from bytes error", e);
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    /**
     * 使用 Tika 解析字符串内容（假设内容是文本编码后的格式）
     */
    private String extractWithTika(String content, String contentType) {
        try {
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            return extractFromBytes(bytes, contentType);
        } catch (Exception e) {
            log.warn("Tika extract failed for contentType={}, fallback to raw content", contentType, e);
            return content;
        }
    }

    /**
     * 从 HTML 提取纯文本（去除标签）
     */
    public String extractHtmlText(String html) {
        if (html == null) return "";
        return HTML_TAG_PATTERN.matcher(html).replaceAll(" ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * 从 JSON 提取关键文本（去除语法字符）
     */
    public String extractJsonText(String json) {
        if (json == null) return "";
        return JSON_NOISE_PATTERN.matcher(json).replaceAll(" ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * 按句子分块文本
     *
     * @param text          纯文本
     * @param chunkSize     每块最大字符数
     * @param chunkOverlap  块间重叠字符数
     */
    public List<String> splitText(String text, int chunkSize, int chunkOverlap) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        if (text.length() <= chunkSize) {
            return List.of(text);
        }

        java.util.List<String> chunks = new java.util.ArrayList<>();
        // 按句子分割（中文：。！？； 英文：.!?;）
        String[] sentences = text.split("(?<=[。！？；.!?;])\\s*");
        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() > chunkSize) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    // 重叠部分
                    String overlap = currentChunk.toString();
                    currentChunk = new StringBuilder();
                    int start = Math.max(0, overlap.length() - chunkOverlap);
                    currentChunk.append(overlap.substring(start));
                }
            }
            currentChunk.append(sentence.trim()).append("。");
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    /**
     * 估算 token 数量（按中文字符 ≈ 1 token，英文 ≈ 4 字符/token）
     */
    public int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) return 0;
        return (int) Math.ceil(text.length() / 4.0);
    }
}
