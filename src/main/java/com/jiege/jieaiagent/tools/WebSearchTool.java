package com.jiege.jieaiagent.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 网页搜索工具 —— 格式化搜索结果，提取标题/链接/摘要
 */
public class WebSearchTool {

    private static final String SEARCH_API_URL = "https://www.searchapi.io/api/v1/search";
    private static final int MAX_RESULTS = 5;
    private static final int TIMEOUT_MS = 15_000;

    private final String apiKey;

    public WebSearchTool(String apiKey) {
        this.apiKey = apiKey;
    }

    @Tool(description = "Search for information from Baidu Search Engine")
    public String searchWeb(
            @ToolParam(description = "Search query keyword") String query) {

        if (StrUtil.isBlank(query)) {
            return "错误：搜索关键词不能为空。";
        }

        try {
            String response = HttpRequest.get(SEARCH_API_URL)
                    .form("q", query)
                    .form("api_key", apiKey)
                    .form("engine", "baidu")
                    .timeout(TIMEOUT_MS)
                    .execute()
                    .body();

            JSONObject json = JSONUtil.parseObj(response);

            // 检查 API 层面错误
            if (json.containsKey("error")) {
                return "搜索失败（API 错误）：" + json.getStr("error");
            }

            JSONArray results = json.getJSONArray("organic_results");
            if (results == null || results.isEmpty()) {
                return "搜索完成，但未找到相关结果，请尝试换个关键词。";
            }

            int count = Math.min(results.size(), MAX_RESULTS);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < count; i++) {
                JSONObject item = results.getJSONObject(i);
                int num = i + 1;
                sb.append(num).append(". ").append(item.getStr("title", "无标题")).append("\n");
                sb.append("   链接：").append(item.getStr("link", "无")).append("\n");
                String snippet = item.getStr("snippet", "");
                if (StrUtil.isNotBlank(snippet)) {
                    // 去除 HTML 标签，限制单条摘要长度
                    snippet = snippet.replaceAll("<[^>]+>", "").trim();
                    if (snippet.length() > 300) {
                        snippet = snippet.substring(0, 300) + "...";
                    }
                    sb.append("   摘要：").append(snippet).append("\n");
                }
                sb.append("\n");
            }

            return sb.toString().trim();
        } catch (Exception e) {
            // Hutool 将超时异常包在 HttpException 里，从 cause 链判断
            if (isTimeout(e)) {
                return "错误：搜索请求超时（" + TIMEOUT_MS / 1000 + "秒），请稍后重试。";
            }
            return "搜索失败：" + e.getMessage() + "。请稍后重试或更换关键词。";
        }
    }

    /** 递归检查异常链中是否包含超时 */
    private boolean isTimeout(Throwable t) {
        if (t == null) return false;
        if (t instanceof java.net.SocketTimeoutException) return true;
        String msg = t.getMessage();
        if (msg != null && (msg.contains("timeout") || msg.contains("超时") || msg.contains("timed out"))) {
            return true;
        }
        return isTimeout(t.getCause());
    }
}
