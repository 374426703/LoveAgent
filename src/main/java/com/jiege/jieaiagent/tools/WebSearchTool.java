package com.jiege.jieaiagent.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class WebSearchTool {

    private static final String SEARCH_API_URL = "https://www.searchapi.io/api/v1/search";
    private static final int MAX_RESULTS = 5;
    private static final int TIMEOUT_MS = 12_000;

    private final String apiKey;

    public WebSearchTool(String apiKey) {
        this.apiKey = apiKey;
    }

    @Tool(description = "搜索网页信息，返回标题、链接和摘要")
    public String searchWeb(@ToolParam(description = "搜索关键词") String query) {
        if (StrUtil.isBlank(query)) {
            return "错误：搜索关键词不能为空";
        }

        if (StrUtil.isNotBlank(apiKey) && !"your-api-key".equals(apiKey)) {
            String result = searchViaApi(query);
            if (result != null && !result.startsWith("错误")) {
                return result;
            }
        }

        return searchViaDuckDuckGo(query);
    }

    private String searchViaApi(String query) {
        try {
            String response = HttpRequest.get(SEARCH_API_URL)
                    .form("q", query)
                    .form("api_key", apiKey)
                    .form("engine", "baidu")
                    .timeout(TIMEOUT_MS)
                    .execute()
                    .body();

            JSONObject json = JSONUtil.parseObj(response);
            if (json.containsKey("error")) {
                return "搜索失败（API错误）：" + json.getStr("error");
            }

            JSONArray results = json.getJSONArray("organic_results");
            if (results == null || results.isEmpty()) {
                return null;
            }

            return formatResults(results);
        } catch (Exception e) {
            return "搜索API异常：" + e.getMessage();
        }
    }

    private String searchViaDuckDuckGo(String query) {
        try {
            String url = "https://html.duckduckgo.com/html/?q=" + URLUtil.encode(query);
            String html = HttpRequest.get(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(TIMEOUT_MS)
                    .execute()
                    .body();

            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(html);
            org.jsoup.select.Elements resultEls = doc.select(".result");
            if (resultEls.isEmpty()) {
                return "搜索完成，但未找到相关结果，请尝试换个关键词";
            }

            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (org.jsoup.nodes.Element el : resultEls) {
                if (count >= MAX_RESULTS) break;
                org.jsoup.nodes.Element linkEl = el.selectFirst(".result__a");
                org.jsoup.nodes.Element snippetEl = el.selectFirst(".result__snippet");
                if (linkEl == null) continue;

                count++;
                String title = linkEl.text();
                String href = extractDdgUrl(linkEl.attr("href"));
                String snippet = snippetEl != null ? snippetEl.text() : "";

                sb.append(count).append(". ").append(title).append("\n");
                sb.append("   链接：").append(href).append("\n");
                if (!snippet.isBlank()) {
                    sb.append("   摘要：").append(snippet.length() > 300
                            ? snippet.substring(0, 300) + "..." : snippet).append("\n");
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "搜索失败：" + e.getMessage() + "。请稍后重试或更换关键词";
        }
    }

    private String extractDdgUrl(String href) {
        if (href == null) return "无";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("uddg=(https?://[^&]+)").matcher(href);
        if (m.find()) {
            return URLUtil.decode(m.group(1));
        }
        return href;
    }

    private String formatResults(JSONArray results) {
        int count = Math.min(results.size(), MAX_RESULTS);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            JSONObject item = results.getJSONObject(i);
            sb.append(i + 1).append(". ").append(item.getStr("title", "无标题")).append("\n");
            sb.append("   链接：").append(item.getStr("link", "无")).append("\n");
            String snippet = item.getStr("snippet", "");
            if (StrUtil.isNotBlank(snippet)) {
                snippet = snippet.replaceAll("<[^>]+>", "").trim();
                if (snippet.length() > 300) snippet = snippet.substring(0, 300) + "...";
                sb.append("   摘要：").append(snippet).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }
}
