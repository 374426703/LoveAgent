package com.jiege.jieaiagent.tools;

import cn.hutool.core.util.StrUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 网页抓取工具 —— 基于 Readability/Boilerpipe 思路的内容提取
 *
 * <p>核心策略：
 * <ol>
 *   <li>模拟浏览器请求，获取页面</li>
 *   <li>剥离噪声标签（script/style/nav/footer/广告等）</li>
 *   <li>对候选容器打分（文本密度 × 链接惩罚），定位正文区域</li>
 *   <li>提取纯文本，截断到合理长度</li>
 *   <li>超时时重试一次（切换协议 + 更长超时）</li>
 * </ol>
 */
public class WebScrapingTool {

    private static final int MAX_TEXT_LENGTH = 4000;
    private static final int TIMEOUT_MS = 15_000;
    private static final int RETRY_TIMEOUT_MS = 25_000;
    private static final int MAX_BODY_BYTES = 512 * 1024;

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
          + "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";

    /** 噪声标签 / 类名 / ID 合集 */
    private static final String[] NOISE_SELECTORS = {
            "script", "style", "noscript", "iframe", "svg", "canvas",
            "nav", "footer", "header", "aside", "form",
            "[role=navigation]", "[role=banner]", "[role=complementary]",
            ".nav", ".navbar", ".navigation", ".menu", ".sidebar",
            ".footer", ".header", ".comment", ".comments",
            ".ad", ".ads", ".advertisement", ".advert",
            ".social", ".share", ".sharing", ".related",
            ".breadcrumb", ".pagination", ".cookie", ".popup",
            "#nav", "#footer", "#header", "#sidebar", "#comments", "#menu",
    };

    @Tool(description = """
            Scrape and extract the main text content from a web page.
            This tool automatically removes ads, navigation, and other noise,
            keeping only the meaningful article text.
            """)
    public String scrapeWebPage(
            @ToolParam(description = "URL of the web page to scrape") String url) {

        if (StrUtil.isBlank(url)) {
            return "错误：URL 为空。";
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        String result = fetchAndExtract(url, TIMEOUT_MS);
        if (result != null && !result.startsWith("错误") && !result.startsWith("请求超时")) {
            return result;
        }

        // 重试：切换协议 + 更长超时
        String retryUrl = url.startsWith("https://")
                ? url.replaceFirst("https://", "http://")
                : url;
        return fetchAndExtract(retryUrl, RETRY_TIMEOUT_MS);
    }

    // ────────────── 核心流程 ──────────────

    private String fetchAndExtract(String url, int timeoutMs) {
        Document doc;
        try {
            doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Cache-Control", "no-cache")
                    .header("Connection", "keep-alive")
                    .header("DNT", "1")
                    .timeout(timeoutMs)
                    .maxBodySize(MAX_BODY_BYTES)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .get();
        } catch (Exception e) {
            return classifyError(e);
        }

        // Step 1: 剥离噪声
        removeNoise(doc);

        // Step 2: 定位正文区域
        Element mainBlock = findMainContent(doc);

        // Step 3: 提取文本
        String title = doc.title();
        String bodyText = (mainBlock != null)
                ? extractReadableText(mainBlock)
                : extractReadableText(doc.body() != null ? doc.body() : doc);

        bodyText = cleanWhitespace(bodyText);

        if (bodyText.length() > MAX_TEXT_LENGTH) {
            bodyText = bodyText.substring(0, MAX_TEXT_LENGTH) + "...（内容已截断）";
        }

        // Step 4: 组装结果
        StringBuilder sb = new StringBuilder();
        if (StrUtil.isNotBlank(title)) {
            sb.append("标题：").append(title.trim()).append("\n\n");
        }
        sb.append(bodyText);

        if (StrUtil.isBlank(bodyText)) {
            return "抓取完成，但该页面未提取到正文文本。可能是：① 纯动态渲染页面（如SPA应用）；② 页面内容为图片/视频。建议改用搜索工具获取信息。";
        }

        return sb.toString();
    }

    // ────────────── 噪声剥离 ──────────────

    /** 移除所有噪声元素 */
    private void removeNoise(Document doc) {
        for (String selector : NOISE_SELECTORS) {
            try {
                Elements elements = doc.select(selector);
                elements.remove();
            } catch (Exception ignored) {
                // 某些选择器可能解析失败，跳过
            }
        }
        // 移除空元素（无文本且无子元素）
        doc.select("*").stream()
                .filter(el -> el.ownText().isBlank() && el.children().isEmpty()
                        && !el.is("br,hr,img,video,audio"))
                .forEach(Element::remove);
    }

    // ────────────── 正文定位（Readability 风格打分） ──────────────

    /**
     * 对候选块打分，返回得分最高的正文容器。
     * 打分公式：文本长度 × log(1 + 文本密度) ÷ (1 + 链接密度²)
     */
    private Element findMainContent(Document doc) {
        // 优先语义标签
        Element article = doc.selectFirst("article");
        if (article != null && hasSubstantialText(article, 200)) return article;

        Element main = doc.selectFirst("main");
        if (main != null && hasSubstantialText(main, 200)) return main;

        // 去除 body 级别的噪声后再打分
        Element body = doc.body();
        if (body == null) return null;

        Element best = null;
        double bestScore = 0;

        // 遍历所有块级容器
        Elements candidates = body.select(
                "div,section,article,main,p,blockquote,pre,li,td,th,dd,dt");
        for (Element el : candidates) {
            double score = scoreBlock(el);
            if (score > bestScore) {
                bestScore = score;
                best = el;
            }
        }

        // 如果最佳得分太低，直接用 body
        if (bestScore < 50 && body != null && hasSubstantialText(body, 100)) {
            return body;
        }
        return best;
    }

    /** 对单个块打分 */
    private double scoreBlock(Element el) {
        String text = el.text();
        int textLen = text.length();
        if (textLen < 80) return 0;

        String html = el.html();
        int htmlLen = Math.max(html.length(), 1);

        // 文本密度：文本占 HTML 的比例
        double textDensity = (double) textLen / htmlLen;

        // 链接惩罚：链接文本越多，越可能是导航/目录
        Elements links = el.select("a");
        int linkTextLen = 0;
        for (Element a : links) {
            linkTextLen += a.text().length();
        }
        double linkRatio = (double) linkTextLen / Math.max(textLen, 1);

        // 得分
        double densityBonus = Math.log(1 + textDensity * 10);
        double linkPenalty = 1.0 / (1.0 + linkRatio * linkRatio * 5);
        return textLen * densityBonus * linkPenalty;
    }

    /** 判断元素是否有足够文本 */
    private boolean hasSubstantialText(Element el, int minLen) {
        return el != null && el.text().length() >= minLen;
    }

    // ────────────── 文本提取 ──────────────

    /**
     * 从元素中提取可读文本，保留段落结构。
     * 遍历子节点，在块级元素间插入换行。
     */
    private String extractReadableText(Element root) {
        StringBuilder sb = new StringBuilder();
        extractTextFromNode(root, sb, true);
        return sb.toString();
    }

    private void extractTextFromNode(Node node, StringBuilder sb, boolean isTopLevel) {
        if (node instanceof TextNode) {
            String text = ((TextNode) node).text();
            if (!text.isBlank()) {
                sb.append(text.trim());
            }
            return;
        }
        if (!(node instanceof Element el)) return;

        String tag = el.tagName().toLowerCase();

        // 跳过噪声标签（防御性再次检查）
        if (tag.matches("script|style|noscript|iframe|svg|canvas|nav|footer|form")) return;

        // 块级标签前后换行
        boolean block = tag.matches(
                "p|div|section|article|header|h[1-6]|li|tr|blockquote|pre|br|hr|table|ul|ol|dl|dd|dt|figure|figcaption|main|aside");

        if (block && !sb.isEmpty() && sb.charAt(sb.length() - 1) != '\n') {
            sb.append('\n');
        }

        for (Node child : el.childNodes()) {
            extractTextFromNode(child, sb, false);
        }

        // 标题额外换行
        if (tag.matches("h[1-6]|p|div|section|article|blockquote")) {
            if (!sb.isEmpty() && sb.charAt(sb.length() - 1) != '\n') {
                sb.append('\n');
            }
        }
    }

    // ────────────── 工具方法 ──────────────

    /** 合并连续空格、去首尾空白、合并多余空行 */
    private String cleanWhitespace(String text) {
        if (text == null) return "";
        // 合并空格
        text = text.replaceAll("[ \\t\\u00A0]+", " ");
        // 多余换行合并为双换行
        text = text.replaceAll("\\n{3,}", "\n\n");
        // 纯空格行清理
        text = text.replaceAll("(?m)^[ \\t]+$", "");
        return text.trim();
    }

    private String classifyError(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        Throwable cause = e;
        while (cause != null) {
            String cls = cause.getClass().getSimpleName();
            if (cls.contains("Timeout") || msg.contains("timeout")
                    || msg.contains("超时") || msg.contains("timed out")) {
                return "请求超时，目标网站响应过慢，建议换用搜索工具获取信息。";
            }
            if (cls.contains("UnknownHost") || msg.contains("UnknownHost")) {
                return "无法解析域名，请检查 URL 是否正确。";
            }
            if (cls.contains("SSL") || msg.contains("SSL")) {
                return "SSL 证书验证失败，该网站不支持 HTTPS。";
            }
            cause = cause.getCause();
        }
        return "抓取失败：" + msg + "。可尝试通过搜索工具获取信息。";
    }
}
