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

public class WebScrapingTool {

    private static final int MAX_TEXT_LENGTH = 6000;
    private static final int TIMEOUT_MS = 12_000;
    private static final int MAX_BODY_BYTES = 512 * 1024;

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
          + "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";

    private static final String[] NOISE_SELECTORS = {
            "script", "style", "noscript", "iframe", "svg", "canvas",
            "nav", "footer", "header", "aside", "form",
            "[role=navigation]", "[role=banner]", "[role=complementary]",
            ".nav", ".navbar", ".navigation", ".menu", ".sidebar",
            ".footer", ".header", ".comment", ".comments",
            ".ad", ".ads", ".advertisement",
            ".social", ".share", ".related",
            ".breadcrumb", ".pagination", ".cookie", ".popup",
    };

    @Tool(description = "抓取网页正文内容，自动去除广告、导航等噪声")
    public String scrapeWebPage(@ToolParam(description = "要抓取的网页URL") String url) {
        if (StrUtil.isBlank(url)) return "错误：URL为空";
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://" + url;

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .timeout(TIMEOUT_MS)
                    .maxBodySize(MAX_BODY_BYTES)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .get();

            removeNoise(doc);
            Element mainBlock = findMainContent(doc);
            String title = doc.title();
            String bodyText = mainBlock != null
                    ? extractText(mainBlock)
                    : extractText(doc.body() != null ? doc.body() : doc);

            bodyText = cleanWhitespace(bodyText);
            if (bodyText.length() > MAX_TEXT_LENGTH) {
                bodyText = bodyText.substring(0, MAX_TEXT_LENGTH) + "...（内容已截断）";
            }

            StringBuilder sb = new StringBuilder();
            if (StrUtil.isNotBlank(title)) sb.append("标题：").append(title.trim()).append("\n\n");
            sb.append(bodyText);

            if (StrUtil.isBlank(bodyText)) {
                return "未提取到正文，可能是动态渲染页面，建议用搜索工具获取信息";
            }
            return sb.toString();
        } catch (Exception e) {
            return "抓取失败：" + classifyError(e);
        }
    }

    private void removeNoise(Document doc) {
        for (String s : NOISE_SELECTORS) {
            try { doc.select(s).remove(); } catch (Exception ignored) {}
        }
    }

    private Element findMainContent(Document doc) {
        Element article = doc.selectFirst("article");
        if (article != null && article.text().length() >= 200) return article;
        Element main = doc.selectFirst("main");
        if (main != null && main.text().length() >= 200) return main;
        Element body = doc.body();
        if (body == null) return null;

        double bestScore = 0;
        Element best = null;
        for (Element el : body.select("div,section,article,main,p,blockquote,pre,li,td,th")) {
            double score = score(el);
            if (score > bestScore) { bestScore = score; best = el; }
        }
        return bestScore > 50 ? best : (body.text().length() >= 100 ? body : null);
    }

    private double score(Element el) {
        int tl = el.text().length();
        if (tl < 80) return 0;
        int ll = 0;
        for (Element a : el.select("a")) ll += a.text().length();
        double density = (double) tl / Math.max(el.html().length(), 1);
        double linkPenalty = 1.0 / (1.0 + Math.pow((double) ll / tl, 2) * 5);
        return tl * Math.log(1 + density * 10) * linkPenalty;
    }

    private String extractText(Element root) {
        StringBuilder sb = new StringBuilder();
        walk(root, sb);
        return sb.toString();
    }

    private void walk(Node node, StringBuilder sb) {
        if (node instanceof TextNode) {
            String t = ((TextNode) node).text();
            if (!t.isBlank()) sb.append(t.trim());
            return;
        }
        if (!(node instanceof Element el)) return;
        String tag = el.tagName().toLowerCase();
        if (tag.matches("script|style|noscript|iframe|svg|canvas|nav|footer|form")) return;

        boolean block = tag.matches("p|div|section|article|h[1-6]|li|tr|blockquote|pre|br|hr|table|ul|ol|dl|figure|main|aside");
        if (block && !sb.isEmpty() && sb.charAt(sb.length() - 1) != '\n') sb.append('\n');

        for (Node child : el.childNodes()) walk(child, sb);
        if (tag.matches("h[1-6]|p|div|section|article|blockquote") && !sb.isEmpty() && sb.charAt(sb.length() - 1) != '\n')
            sb.append('\n');
    }

    private String cleanWhitespace(String text) {
        return text == null ? "" : text.replaceAll("[ \\t\\u00A0]+", " ")
                .replaceAll("\\n{3,}", "\n\n").replaceAll("(?m)^[ \\t]+$", "").trim();
    }

    private String classifyError(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        for (Throwable t = e; t != null; t = t.getCause()) {
            String cls = t.getClass().getSimpleName();
            if (cls.contains("Timeout") || msg.contains("timeout") || msg.contains("超时")) return "请求超时";
            if (cls.contains("UnknownHost")) return "无法解析域名，请检查URL";
            if (cls.contains("SSL") || msg.contains("SSL")) return "SSL连接失败";
        }
        return msg;
    }
}
