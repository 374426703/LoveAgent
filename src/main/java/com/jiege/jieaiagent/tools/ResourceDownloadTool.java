package com.jiege.jieaiagent.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.jiege.jieaiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class ResourceDownloadTool {

    private static final int TIMEOUT_MS = 20_000;
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    @Tool(description = "从URL下载资源文件")
    public String downloadResource(
            @ToolParam(description = "资源URL") String url,
            @ToolParam(description = "保存的文件名") String fileName) {

        if (url == null || url.isBlank()) return "错误：URL为空";
        if (fileName == null || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return "错误：文件名不合法";
        }

        String fileDir = FileConstant.FILE_SAVE_DIR + "/download";

        try {
            FileUtil.mkdir(fileDir);
            fileName = uniqueName(fileDir, fileName);
            String filePath = fileDir + "/" + fileName;

            HttpResponse resp = HttpRequest.get(url)
                    .timeout(TIMEOUT_MS)
                    .execute();
            if (!resp.isOk()) {
                return "下载失败：HTTP " + resp.getStatus();
            }

            long len = resp.contentLength();
            if (len > MAX_FILE_SIZE) {
                return "下载失败：文件超过50MB限制";
            }

            byte[] bytes = resp.bodyBytes();
            FileUtil.writeBytes(bytes, filePath);
            return "Resource downloaded successfully to: " + filePath;
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("timeout") || msg.contains("超时"))) {
                return "下载超时（" + TIMEOUT_MS / 1000 + "秒）";
            }
            return "下载失败：" + msg;
        }
    }

    private String uniqueName(String dir, String name) {
        String base = name.contains(".") ? name.replaceAll("\\.[^.]+$", "") : name;
        String ext = name.contains(".") ? name.substring(name.lastIndexOf(".")) : "";
        String ts = new java.text.SimpleDateFormat("yyyyMMddHHmmssSSS").format(new java.util.Date());
        String candidate = dir + "/" + base + "_" + ts + ext;
        if (!new java.io.File(candidate).exists()) return base + "_" + ts + ext;
        for (int i = 1; i < 100; i++) {
            candidate = dir + "/" + base + "_" + ts + "_" + i + ext;
            if (!new java.io.File(candidate).exists()) return base + "_" + ts + "_" + i + ext;
        }
        return base + "_" + ts + ext;
    }
}
