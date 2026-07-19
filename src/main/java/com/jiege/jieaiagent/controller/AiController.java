package com.jiege.jieaiagent.controller;

import com.jiege.jieaiagent.agent.JieAgent;
import com.jiege.jieaiagent.app.LoveApp;
import com.jiege.jieaiagent.constant.FileConstant;
import com.jiege.jieaiagent.mapper.ConversationMapper;
import com.jiege.jieaiagent.model.Conversation;
import com.jiege.jieaiagent.model.User;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private LoveApp loveApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel chatModel;

    @Resource
    private ConversationMapper conversationMapper;

    // AI 调用 MCP 服务
    @Resource
    private SyncMcpToolCallbackProvider toolCallbackProvider;

    @GetMapping(value = "/love_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppSSE(HttpServletRequest request, String message, String chatId) {
        ensureConversationExists(request, message, chatId, "love_app");
        return loveApp.doChatByStream(message, chatId);
    }

    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(HttpServletRequest request, String message, String chatId) {
        ensureConversationExists(request, message, chatId, "super_agent");
        JieAgent JieAgent = new JieAgent(allTools, chatModel);
        return JieAgent.runStream(message);
    }

    /**
     * 文件下载接口 —— 下载 Agent 生成的文件（PDF、图片等）
     * 路径格式: /ai/files/{subDir}/{fileName}
     */
    @GetMapping("/files/**")
    public ResponseEntity<byte[]> downloadGeneratedFile(HttpServletRequest request) throws IOException {
        String requestUri = request.getRequestURI();
        int prefixIndex = requestUri.indexOf("/ai/files/");
        if (prefixIndex < 0) {
            return ResponseEntity.notFound().build();
        }
        String relativePath = requestUri.substring(prefixIndex + "/ai/files/".length());

        // 安全检查：防止路径穿越
        if (relativePath.contains("..") || relativePath.contains("\\\\")) {
            return ResponseEntity.badRequest().build();
        }

        File file = new File(FileConstant.FILE_SAVE_DIR, relativePath);
        File canonicalFile = file.getCanonicalFile();
        Path saveDir = Paths.get(FileConstant.FILE_SAVE_DIR).toAbsolutePath().normalize();

        // 确保解析后的文件仍在允许的目录下
        if (!canonicalFile.toPath().normalize().startsWith(saveDir)) {
            return ResponseEntity.badRequest().build();
        }

        if (!canonicalFile.exists() || !canonicalFile.isFile()) {
            return ResponseEntity.notFound().build();
        }

        // 根据扩展名确定 Content-Type
        String contentType = Files.probeContentType(canonicalFile.toPath());
        if (contentType == null) {
            contentType = determineContentTypeByExtension(canonicalFile.getName());
        }

        byte[] bytes = Files.readAllBytes(canonicalFile.toPath());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + canonicalFile.getName() + "\"")
                .body(bytes);
    }

    /**
     * 根据文件扩展名判断 Content-Type（Files.probeContentType 的补充）
     */
    private String determineContentTypeByExtension(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".txt") || lower.endsWith(".md")) return "text/plain; charset=utf-8";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=utf-8";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".xml")) return "application/xml";
        return "application/octet-stream";
    }

    private void ensureConversationExists(HttpServletRequest request, String message, String chatId, String appType) {
        if (chatId == null || chatId.isBlank()) return;
        User user = (User) request.getAttribute("currentUser");
        if (user == null || user.getId() == 0) return;

        String title = buildTitle(message);

        Conversation existing = conversationMapper.selectById(chatId);
        if (existing == null) {
            Conversation conv = Conversation.builder()
                .id(chatId)
                .userId(user.getId())
                .title(title)
                .appType(appType)
                .build();
            conversationMapper.insert(conv);
        } else if ("新对话".equals(existing.getTitle())) {
            existing.setTitle(title);
            conversationMapper.updateById(existing);
        }
    }

    private String buildTitle(String message) {
        if (message == null || message.isBlank()) return "新对话";
        String cleaned = message.replace('\n', ' ').replace('\r', ' ').trim();
        return cleaned.length() > 30 ? cleaned.substring(0, 30) + "..." : cleaned;
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadByPath(@RequestParam("path") String path) throws IOException {
        String relativePath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        if (relativePath.contains("..") || relativePath.contains("\\\\")) {
            return ResponseEntity.badRequest().build();
        }

        File file = new File(FileConstant.FILE_SAVE_DIR, relativePath);
        File canonicalFile = file.getCanonicalFile();
        Path saveDir = Paths.get(FileConstant.FILE_SAVE_DIR).toAbsolutePath().normalize();

        if (!canonicalFile.toPath().normalize().startsWith(saveDir)) {
            return ResponseEntity.badRequest().build();
        }
        if (!canonicalFile.exists() || !canonicalFile.isFile()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(canonicalFile.toPath());
        if (contentType == null) {
            contentType = canonicalFile.getName().toLowerCase().endsWith(".pdf")
                    ? "application/pdf" : "application/octet-stream";
        }

        byte[] bytes = Files.readAllBytes(canonicalFile.toPath());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + canonicalFile.getName() + "\"")
                .body(bytes);
    }
}
