package com.jiege.jieaiagent.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.jiege.jieaiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;

public class PDFGenerationTool {

    private static final String[] CJK_FONT_CANDIDATES = {
            "C:/Windows/Fonts/simsun.ttc", "C:/Windows/Fonts/simhei.ttf",
            "C:/Windows/Fonts/msyh.ttc", "C:/Windows/Fonts/msyhbd.ttc",
            "C:/Windows/Fonts/Deng.ttf", "C:/Windows/Fonts/mingliu.ttc",
            "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
            "/System/Library/Fonts/PingFang.ttc",
    };

    private static volatile PdfFont cachedFont;
    private static volatile boolean fontSearched;

    @Tool(description = "生成PDF文件。内容支持Markdown格式：## 标题、**加粗**、- 列表项")
    public String generatePDF(
            @ToolParam(description = "PDF文件名，以.pdf结尾") String fileName,
            @ToolParam(description = "PDF正文内容") String content) {

        String safeName = sanitizeFileName(fileName);
        safeName = uniqueName(safeName);
        String fileDir = FileConstant.FILE_SAVE_DIR + "/pdf";
        String filePath = fileDir + "/" + safeName;

        if (StrUtil.isBlank(content)) content = "（无内容）";

        try {
            FileUtil.mkdir(fileDir);
            PdfFont font = loadCjkFont();

            try (PdfWriter writer = new PdfWriter(filePath);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {

                document.setFont(font);

                String[] sections = content.split("\n\n+");
                for (String section : sections) {
                    String trimmed = section.trim();
                    if (trimmed.isEmpty()) continue;

                    if (trimmed.startsWith("## ")) {
                        document.add(new Paragraph(trimmed.substring(3).trim())
                                .setFontSize(16).setMarginTop(12));
                    } else if (trimmed.startsWith("# ")) {
                        document.add(new Paragraph(trimmed.substring(2).trim())
                                .setFontSize(20).setMarginBottom(16));
                    } else if (trimmed.startsWith("### ")) {
                        document.add(new Paragraph(trimmed.substring(4).trim())
                                .setFontSize(14).setMarginTop(8));
                    } else if (trimmed.startsWith("- ")) {
                        String[] lines = trimmed.split("\n");
                        for (String line : lines) {
                            String itemText = line.replaceFirst("^-\\s*", "").trim();
                            if (!itemText.isEmpty()) {
                                document.add(new Paragraph("  • " + itemText)
                                        .setFontSize(11).setMarginLeft(12));
                            }
                        }
                    } else {
                        document.add(new Paragraph(trimmed).setFontSize(11));
                    }
                }
            }
            return "PDF generated successfully to: " + filePath;
        } catch (Exception e) {
            return "Error generating PDF: " + e.getMessage();
        }
    }

    private String uniqueName(String safeName) {
        String base = safeName.replaceAll("\\.pdf$", "");
        String ts = new java.text.SimpleDateFormat("yyyyMMddHHmmssSSS").format(new java.util.Date());
        String dir = FileConstant.FILE_SAVE_DIR + "/pdf";
        String candidate = dir + "/" + base + "_" + ts + ".pdf";
        if (!new java.io.File(candidate).exists()) {
            return base + "_" + ts + ".pdf";
        }
        for (int i = 1; i < 100; i++) {
            candidate = dir + "/" + base + "_" + ts + "_" + i + ".pdf";
            if (!new java.io.File(candidate).exists()) {
                return base + "_" + ts + "_" + i + ".pdf";
            }
        }
        return base + "_" + ts + ".pdf";
    }

    private String sanitizeFileName(String fileName) {
        if (StrUtil.isBlank(fileName)) return "document.pdf";
        String safe = fileName.replace('\\', '_').replace('/', '_')
                .replaceAll("[\"'\"''「」『』【】《》＂＇]", "_")
                .replaceAll("[:*?<>|]", "_")
                .replaceAll("_{2,}", "_")
                .replaceAll("^[_\\s]+|[_\\s]+$", "").trim();
        if (safe.isEmpty()) return "document.pdf";
        if (!safe.toLowerCase().endsWith(".pdf")) safe += ".pdf";
        return safe;
    }

    private PdfFont loadCjkFont() throws Exception {
        if (fontSearched && cachedFont != null) return cachedFont;
        synchronized (PDFGenerationTool.class) {
            if (fontSearched && cachedFont != null) return cachedFont;
            fontSearched = true;

            try {
                cachedFont = PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H");
                return cachedFont;
            } catch (Exception ignored) {}

            for (String path : CJK_FONT_CANDIDATES) {
                File f = new File(path);
                if (f.exists() && f.isFile()) {
                    try {
                        cachedFont = PdfFontFactory.createFont(f.getAbsolutePath(),
                                PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
                        return cachedFont;
                    } catch (Exception ignored) {}
                }
            }
            throw new Exception("未找到中文字体，请将 simsun.ttf 复制到 src/main/resources/fonts/ 目录");
        }
    }
}
