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

/**
 * PDF 生成工具 —— 穷举系统字体，确保中文正常渲染
 */
public class PDFGenerationTool {

    /** 系统 CJK 字体候选列表（按优先级排列） */
    private static final String[] CJK_FONT_CANDIDATES = {
            // ── Windows 系统字体 ──
            "C:/Windows/Fonts/simsun.ttc",       // 宋体
            "C:/Windows/Fonts/simhei.ttf",       // 黑体
            "C:/Windows/Fonts/simkai.ttf",       // 楷体
            "C:/Windows/Fonts/msyh.ttc",         // 微软雅黑
            "C:/Windows/Fonts/msyhbd.ttc",       // 微软雅黑 Bold
            "C:/Windows/Fonts/msyhl.ttc",        // 微软雅黑 Light
            "C:/Windows/Fonts/Deng.ttf",         // 等线
            "C:/Windows/Fonts/Dengb.ttf",        // 等线 Bold
            "C:/Windows/Fonts/Dengl.ttf",        // 等线 Light
            "C:/Windows/Fonts/mingliu.ttc",      // 细明体
            "C:/Windows/Fonts/STSONG.TTF",       // 华文宋体
            "C:/Windows/Fonts/STKAITI.TTF",      // 华文楷体
            "C:/Windows/Fonts/STHEITI.TTF",      // 华文黑体
            "C:/Windows/Fonts/STFANGSO.TTF",     // 华文仿宋
            // ── Linux / macOS ──
            "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
            "/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf",
            "/System/Library/Fonts/PingFang.ttc",
            "/System/Library/Fonts/STHeiti Light.ttc",
            // ── 项目内置字体（用户可放入 resources/fonts/） ──
            "src/main/resources/fonts/simsun.ttf",
            "src/main/resources/fonts/NotoSansSC-Regular.ttf",
    };

    /** 首次加载后缓存，避免重复遍历文件系统 */
    private static volatile PdfFont cachedCjkFont;
    private static volatile boolean fontSearched;

    @Tool(description = "Generate a PDF file with given content", returnDirect = false)
    public String generatePDF(
            @ToolParam(description = "Name of the file to save the generated PDF") String fileName,
            @ToolParam(description = "Content to be included in the PDF") String content) {

        String safeName = sanitizeFileName(fileName);
        String fileDir = FileConstant.FILE_SAVE_DIR + "/pdf";
        String filePath = fileDir + "/" + safeName;

        if (StrUtil.isBlank(content)) {
            content = "（无内容）";
        }

        try {
            FileUtil.mkdir(fileDir);
            try (PdfWriter writer = new PdfWriter(filePath);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {

                PdfFont font = loadCjkFont();
                document.setFont(font);
                document.add(new Paragraph(content));
            }
            return "PDF generated successfully to: " + filePath;
        } catch (Exception e) {
            return "Error generating PDF: " + e.getMessage()
                    + ". 请检查内容是否为纯文本、文件名是否合法。";
        }
    }

    // ────────────── 内部方法 ──────────────

    /**
     * 清洗文件名：去非法字符、中文引号、确保以 .pdf 结尾
     */
    private String sanitizeFileName(String fileName) {
        if (StrUtil.isBlank(fileName)) return "document.pdf";
        // 替换路径分隔符
        String safe = fileName.replace('\\', '_').replace('/', '_');
        // 去除各种引号：ASCII 双引号、中文双引号、中文单引号、尖括号等
        safe = safe.replace('"', '_')   // "  ASCII 双引号
                   .replace('“', '_')   // "  左双引号
                   .replace('”', '_')   // "  右双引号
                   .replace('‘', '_')   // '  左单引号
                   .replace('’', '_')   // '  右单引号
                   .replace('「', '_')   // 「
                   .replace('」', '_')   // 」
                   .replace('『', '_')   // 『
                   .replace('』', '_')   // 』
                   .replace('【', '_')   // 【
                   .replace('】', '_')   // 】
                   .replace('《', '_')   // 《
                   .replace('》', '_')   // 》
                   .replace('＂', '_')   // ＂ 全角双引号
                   .replace('＇', '_');  // ＇ 全角单引号
        // 去除其他非法字符
        safe = safe.replaceAll("[:*?<>|]", "_");
        // 去除连续下划线
        safe = safe.replaceAll("_{2,}", "_");
        // 去掉首尾的下划线和空白
        safe = safe.replaceAll("^[_\\s]+|[_\\s]+$", "").trim();
        if (safe.isEmpty()) return "document.pdf";
        if (!safe.toLowerCase().endsWith(".pdf")) safe += ".pdf";
        return safe;
    }

    /**
     * 加载 CJK 字体，按优先级探测：
     * ① font-asian 内置字体（需 scope=compile）
     * ② 系统字体文件（Windows / Linux / macOS 常用路径）
     *
     * @return 可用的中文字体（永不返回 null）
     * @throws Exception 所有字体源均不可用时抛出
     */
    private PdfFont loadCjkFont() throws Exception {
        if (fontSearched && cachedCjkFont != null) {
            return cachedCjkFont;
        }
        synchronized (PDFGenerationTool.class) {
            if (fontSearched && cachedCjkFont != null) {
                return cachedCjkFont;
            }
            fontSearched = true;

            // ① font-asian 内置
            try {
                cachedCjkFont = PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H");
                return cachedCjkFont;
            } catch (Exception ignored) { /* continue */ }

            // ② 遍历系统字体候选
            for (String fontPath : CJK_FONT_CANDIDATES) {
                File file = new File(fontPath);
                if (file.exists() && file.isFile()) {
                    try {
                        cachedCjkFont = PdfFontFactory.createFont(
                                file.getAbsolutePath(),
                                PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
                        return cachedCjkFont;
                    } catch (Exception ignored) { /* try next */ }
                }
            }

            // ③ 所有策略失败 → 抛错，错误消息会返回给 LLM
            throw new Exception(
                    "未找到可用的中文字体。解决方法（任选其一）："
                  + " (1) 将 pom.xml 中 font-asian 的 <scope>test</scope> 删除，重新编译；"
                  + " (2) 将 simsun.ttf 复制到 src/main/resources/fonts/ 目录下。");
        }
    }
}
