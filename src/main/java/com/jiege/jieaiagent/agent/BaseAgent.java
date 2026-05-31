package com.jiege.jieaiagent.agent;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiege.jieaiagent.agent.model.AgentState;
import com.jiege.jieaiagent.constant.FileConstant;
import com.jiege.jieaiagent.dto.SseEvent;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 抽象基础代理类，用于管理代理状态和执行流程。
 * <p>
 * 提供状态转换、内存管理和基于步骤的执行循环的基础功能。
 * 子类必须实现step方法。
 */
@Data
@Slf4j
public abstract class BaseAgent {

    // 核心属性
    private String name;

    // 提示词
    private String systemPrompt;
    private String nextStepPrompt;

    // 代理状态
    private AgentState state = AgentState.IDLE;

    // 执行步骤控制
    private int currentStep = 0;
    private int maxSteps = 10;

    // LLM 大模型
    private org.springframework.ai.chat.client.ChatClient chatClient;

    // Memory 记忆（需要自主维护会话上下文）
    private List<Message> messageList = new ArrayList<>();

    /** 匹配工具返回字符串中的文件路径，如 "successfully to: D:\path\to\file.pdf" */
    private static final Pattern FILE_PATH_PATTERN = Pattern.compile("successfully to:\\s*(.+)$", Pattern.MULTILINE);

    /** JSON 序列化（手动序列化保证 SSE text/event-stream 下也能输出 JSON） */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 运行代理
     *
     * @param userPrompt 用户提示词
     * @return 执行结果
     */
    public String run(String userPrompt) {
        // 1、基础校验
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state: " + this.state);
        }
        if (StrUtil.isBlank(userPrompt)) {
            throw new RuntimeException("Cannot run agent with empty user prompt");
        }
        // 2、执行，更改状态
        this.state = AgentState.RUNNING;
        // 记录消息上下文
        messageList.add(new UserMessage(userPrompt));
        // 保存结果列表
        List<String> results = new ArrayList<>();
        try {
            // 执行循环
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Executing step {}/{}", stepNumber, maxSteps);
                // 单步执行
                String stepResult = step();
                String result = "Step " + stepNumber + ": " + stepResult;
                results.add(result);
            }
            // 检查是否超出步骤限制
            if (currentStep >= maxSteps) {
                state = AgentState.FINISHED;
                results.add("Terminated: Reached max steps (" + maxSteps + ")");
            }
            return String.join("\n", results);
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("error executing agent", e);
            return "执行错误" + e.getMessage();
        } finally {
            // 3、清理资源
            this.cleanup();
        }
    }

    /**
     * 运行代理（流式输出 —— 发送结构化 JSON 事件）
     *
     * @param userPrompt 用户提示词
     * @return 执行结果
     */
    public SseEmitter runStream(String userPrompt) {
        // 创建一个超时时间较长的 SseEmitter
        SseEmitter sseEmitter = new SseEmitter(300000L); // 5 分钟超时
        // 使用线程异步处理，避免阻塞主线程
        CompletableFuture.runAsync(() -> {
            // 1、基础校验
            try {
                if (this.state != AgentState.IDLE) {
                    sendEvent(sseEmitter, SseEvent.error("无法从状态运行代理：" + this.state));
                    sseEmitter.complete();
                    return;
                }
                if (StrUtil.isBlank(userPrompt)) {
                    sendEvent(sseEmitter, SseEvent.error("不能使用空提示词运行代理"));
                    sseEmitter.complete();
                    return;
                }
            } catch (Exception e) {
                sseEmitter.completeWithError(e);
                return;
            }
            // 2、执行，更改状态
            this.state = AgentState.RUNNING;
            // 记录消息上下文
            messageList.add(new UserMessage(userPrompt));
            try {
                // 执行循环
                for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                    int stepNumber = i + 1;
                    currentStep = stepNumber;
                    log.info("Executing step {}/{}", stepNumber, maxSteps);

                    // 单步执行
                    String stepResult = step();

                    // 最终步骤不走工具展示，交给 answer 事件
                    if (state == AgentState.FINISHED) {
                        break;
                    }

                    // 工具调用 → 仅展示"正在调用 XXX 工具"
                    String toolName = getLastToolName(stepResult);
                    if (toolName != null) {
                        sendEvent(sseEmitter, SseEvent.toolCall(toolName,
                                "正在调用 " + toolName + " 工具…"));

                        // 生成了文件 → 额外推送下载链接
                        String filePath = extractFilePath(stepResult);
                        if (filePath != null) {
                            String fileName = Paths.get(filePath).getFileName().toString();
                            String fileUrl = buildFileUrl(filePath);
                            sendEvent(sseEmitter, SseEvent.file(fileName, fileUrl,
                                    "文件 " + fileName + " 已生成"));
                        }
                    }
                }
                // 检查是否超出步骤限制
                if (currentStep >= maxSteps) {
                    state = AgentState.FINISHED;
                    sendEvent(sseEmitter, SseEvent.error("达到最大步骤限制（" + maxSteps + "）"));
                }
                // 发送最终答案：获取最后一条 AI 回复文本
                String finalAnswer = getLastAssistantText();
                if (StrUtil.isNotBlank(finalAnswer)) {
                    sendEvent(sseEmitter, SseEvent.answer(finalAnswer));
                }
                // 发送结束事件
                sendEvent(sseEmitter, SseEvent.done());
                // 正常完成
                sseEmitter.complete();
            } catch (Exception e) {
                state = AgentState.ERROR;
                log.error("error executing agent", e);
                sendEvent(sseEmitter, SseEvent.error("执行错误：" + e.getMessage()));
                sendEvent(sseEmitter, SseEvent.done());
                sseEmitter.complete();
            } finally {
                // 3、清理资源
                this.cleanup();
            }
        });

        // 设置超时回调
        sseEmitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            this.cleanup();
            log.warn("SSE connection timeout");
        });
        // 设置完成回调
        sseEmitter.onCompletion(() -> {
            if (this.state == AgentState.RUNNING) {
                this.state = AgentState.FINISHED;
            }
            this.cleanup();
            log.info("SSE connection completed");
        });
        return sseEmitter;
    }

    /**
     * 定义单个步骤
     *
     * @return
     */
    public abstract String step();

    /**
     * 清理资源
     */
    protected void cleanup() {
        // 子类可以重写此方法来清理资源
    }

    // ========== 辅助方法 ==========

    /**
     * 从消息列表中获取最后一条 AssistantMessage 的文本内容
     */
    private String getLastAssistantText() {
        List<Message> messages = getMessageList();
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if (msg instanceof AssistantMessage assistantMsg) {
                String text = assistantMsg.getText();
                if (StrUtil.isNotBlank(text)) {
                    return text.trim();
                }
            }
        }
        return null;
    }

    /**
     * 从步骤结果字符串中解析工具名称
     * 格式: "工具 xxx 返回的结果：..." → "xxx"
     */
    private String getLastToolName(String stepResult) {
        if (StrUtil.isBlank(stepResult)) {
            return null;
        }
        Pattern p = Pattern.compile("^工具\\s+(\\S+)\\s+返回的结果");
        Matcher m = p.matcher(stepResult);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    /**
     * 从文本中提取生成的文件路径
     * 匹配 "successfully to: /path/to/file" 模式
     */
    private String extractFilePath(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        Matcher m = FILE_PATH_PATTERN.matcher(text);
        if (m.find()) {
            String path = m.group(1).trim();
            // 兜底清理：去掉 LLM 可能附加的各种引号
            path = stripQuotes(path);
            if (StrUtil.isBlank(path)) {
                return null;
            }
            try {
                if (java.nio.file.Files.exists(Paths.get(path))) {
                    return path;
                }
            } catch (Exception ignored) {
                // 路径格式非法（如含非法字符），跳过
            }
        }
        return null;
    }

    /** 去掉字符串两端各种类型的引号 */
    private String stripQuotes(String s) {
        if (s == null) return null;
        // 要移除的引号字符集合
        String quotes = "\"'\"''「」『』【】《》＂＇";
        while (!s.isEmpty() && quotes.indexOf(s.charAt(0)) >= 0) {
            s = s.substring(1);
        }
        while (!s.isEmpty() && quotes.indexOf(s.charAt(s.length() - 1)) >= 0) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    /**
     * 将文件绝对路径转换为前端可访问的相对 URL
     * D:\...\tmp\pdf\report.pdf → /ai/files/pdf/report.pdf
     */
    private String buildFileUrl(String absolutePath) {
        try {
            Path saveDir = Paths.get(FileConstant.FILE_SAVE_DIR).toAbsolutePath().normalize();
            Path filePath = Paths.get(absolutePath).toAbsolutePath().normalize();
            Path relative = saveDir.relativize(filePath);
            return "/ai/files/" + relative.toString().replace("\\", "/");
        } catch (Exception e) {
            log.warn("Failed to build file URL for path: {}", absolutePath, e);
            // 降级：只用文件名
            String fileName = Paths.get(absolutePath).getFileName().toString();
            return "/ai/files/" + fileName;
        }
    }

    /**
     * 安全地向 SseEmitter 发送事件。
     * 先手动序列化为 JSON 字符串再发送，避免 SSE 的 text/event-stream
     * content-type 导致 Jackson converter 不生效、退化为 toString()。
     */
    private void sendEvent(SseEmitter emitter, SseEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            emitter.send(json);
        } catch (IOException e) {
            log.debug("SSE send failed (client may have disconnected): {}", e.getMessage());
        }
    }
}
