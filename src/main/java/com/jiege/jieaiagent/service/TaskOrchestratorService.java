package com.jiege.jieaiagent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiege.jieaiagent.constant.FileConstant;
import com.jiege.jieaiagent.dto.SseEvent;
import com.jiege.jieaiagent.mapper.ChatMessageMapper;
import com.jiege.jieaiagent.model.ChatMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class TaskOrchestratorService {

    private final ChatClient chatClient;
    private final ToolCallback[] allTools;
    private final ToolCallingManager toolCallingManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private ChatMessageMapper chatMessageMapper;

    public TaskOrchestratorService(org.springframework.ai.chat.model.ChatModel chatModel,
                                   ToolCallback[] allTools) {
        this.allTools = allTools;
        this.chatClient = ChatClient.builder(chatModel).build();
        this.toolCallingManager = ToolCallingManager.builder().build();
    }

    public SseEmitter execute(String chatId, String message) {
        SseEmitter emitter = new SseEmitter(600000L);

        CompletableFuture.runAsync(() -> {
            List<Message> history = new ArrayList<>();
            history.add(new UserMessage(message));

            if (chatId != null && !chatId.isBlank()) {
                saveMessage(chatId, message, "USER");
            }

            var chatOptions = ToolCallingChatOptions.builder()
                    .internalToolExecutionEnabled(false)
                    .build();

            StringBuilder finalAnswer = new StringBuilder();
            try {
                for (int step = 0; step < 25; step++) {
                    Prompt prompt = new Prompt(history, chatOptions);
                    ChatResponse chatResponse;
                    try {
                        chatResponse = chatClient.prompt(prompt)
                                .system(getSystemPrompt())
                                .toolCallbacks(allTools)
                                .call()
                                .chatResponse();
                    } catch (Exception e) {
                        log.error("LLM 调用失败", e);
                        send(emitter, SseEvent.error("AI 调用失败: " + e.getMessage()));
                        break;
                    }

                    AssistantMessage assistantMsg = chatResponse.getResult().getOutput();
                    String thinkText = assistantMsg.getText();
                    if (thinkText != null && !thinkText.isBlank()) {
                        send(emitter, SseEvent.thinking(thinkText));
                    }

                    List<AssistantMessage.ToolCall> toolCalls = assistantMsg.getToolCalls();
                    if (toolCalls.isEmpty()) {
                        String reply = assistantMsg.getText();
                        if (reply != null && !reply.isBlank()) {
                            send(emitter, SseEvent.answer(reply));
                            finalAnswer.append(reply);
                        }
                        break;
                    }

                    StringBuilder toolInfo = new StringBuilder();
                    for (AssistantMessage.ToolCall tc : toolCalls) {
                        String name = tc.name();
                        String args = tc.arguments();
                        toolInfo.append("调用工具: ").append(name).append("\n参数: ").append(args);
                        send(emitter, SseEvent.toolCall(name, "正在调用 " + name + " ..."));
                    }
                    log.info("步骤 {}: {}", step + 1, toolInfo);

                    ToolExecutionResult execResult = toolCallingManager.executeToolCalls(prompt, chatResponse);
                    history = execResult.conversationHistory();

                    ToolResponseMessage toolResp = (ToolResponseMessage) history.get(history.size() - 1);
                    for (ToolResponseMessage.ToolResponse r : toolResp.getResponses()) {
                        String result = r.responseData();
                        log.info("工具 {} 返回: {}", r.name(), result != null ? result.substring(0, Math.min(200, result.length())) : "null");

                        if ("generatePDF".equals(r.name()) && result != null) {
                            String filePath = extractFilePath(result);
                            if (filePath != null) {
                                String fileName = java.nio.file.Paths.get(filePath).getFileName().toString();
                                String fileUrl = buildFileUrl(filePath);
                                send(emitter, SseEvent.file(fileName, fileUrl, "PDF 生成完成，点击下载"));
                            }
                        }
                        if ("downloadResource".equals(r.name()) && result != null) {
                            String filePath = extractFilePath(result);
                            if (filePath != null) {
                                String fileName = java.nio.file.Paths.get(filePath).getFileName().toString();
                                String fileUrl = buildFileUrl(filePath);
                                send(emitter, SseEvent.file(fileName, fileUrl, "文件下载完成"));
                            }
                        }
                    }

                    boolean terminated = toolResp.getResponses().stream()
                            .anyMatch(r -> "doTerminate".equals(r.name()));
                    if (terminated) break;
                }
            } catch (Exception e) {
                log.error("任务执行异常", e);
                send(emitter, SseEvent.error("任务执行异常: " + e.getMessage()));
            }

            if (chatId != null && !chatId.isBlank() && finalAnswer.length() > 0) {
                saveMessage(chatId, finalAnswer.toString(), "ASSISTANT");
            }

            send(emitter, SseEvent.done());
            emitter.complete();
        });

        emitter.onTimeout(() -> log.warn("SSE 超时"));
        emitter.onCompletion(() -> log.info("SSE 完成"));
        return emitter;
    }

    private void saveMessage(String chatId, String content, String type) {
        try {
            ChatMessage msg = ChatMessage.builder()
                    .conversationId(chatId)
                    .content(content)
                    .type(type)
                    .timestamp(LocalDateTime.now())
                    .build();
            chatMessageMapper.insert(msg);
        } catch (Exception e) {
            log.error("保存聊天消息失败", e);
        }
    }

    private String getSystemPrompt() {
        return """
                你是超级特工，一个具备工具调用能力的智能助手。
                
                ## 核心规则
                1. 收到任务后先分析制定计划，再逐步执行
                2. 需要最新信息时必须先调用 searchWeb 搜索
                3. 搜索到信息后直接整理，调用 generatePDF 生成文档
                4. PDF内容要结构化（标题、分段），搜索结果标注来源链接
                5. 生成完 PDF 后调用 doTerminate 结束
                6. 不要编造信息，一切以工具返回为准
                7. 工具失败时换关键词重试一次
                
                ## 搜索→PDF 流水线
                用户："查询上海约会地点并生成PDF"
                1. searchWeb("上海约会地点推荐") → 获取搜索结果
                2. 整理搜索结果成结构化内容
                3. generatePDF("上海约会地点推荐.pdf", "整理好的内容...")
                4. doTerminate()
                """;
    }

    private String extractFilePath(String text) {
        if (text == null) return null;
        String marker = "successfully to: ";
        int idx = text.indexOf(marker);
        if (idx < 0) return null;
        String path = text.substring(idx + marker.length()).trim();
        path = path.replaceAll("^[\"'']+|[\"'']+$", "");
        try {
            if (java.nio.file.Files.exists(java.nio.file.Paths.get(path))) {
                return path;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String buildFileUrl(String absolutePath) {
        try {
            java.nio.file.Path savePath = java.nio.file.Paths.get(FileConstant.FILE_SAVE_DIR).toAbsolutePath().normalize();
            java.nio.file.Path filePath = java.nio.file.Paths.get(absolutePath).toAbsolutePath().normalize();
            java.nio.file.Path relative = savePath.relativize(filePath);
            String relStr = relative.toString().replace("\\", "/");
            return "/ai/download?path=" + URLEncoder.encode(relStr, StandardCharsets.UTF_8);
        } catch (Exception e) {
            String fileName = java.nio.file.Paths.get(absolutePath).getFileName().toString();
            return "/ai/download?path=" + fileName;
        }
    }

    private void send(SseEmitter emitter, SseEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            emitter.send(json);
        } catch (IOException e) {
            log.debug("SSE 发送失败: {}", e.getMessage());
        }
    }
}
