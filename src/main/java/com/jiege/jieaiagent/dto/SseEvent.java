package com.jiege.jieaiagent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * SSE 结构化事件 - 用于向前端发送类型化的事件数据
 * <p>
 * 替代原来的纯文本 SSE 消息，前端可根据 type 字段区分事件类型并分别处理：
 * - thinking: AI思考过程
 * - tool_call: 工具调用
 * - file: 文件生成（含下载URL）
 * - answer: AI最终回答
 * - error: 错误
 * - done: 流结束
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SseEvent {

    /**
     * 事件类型
     */
    private String type;

    /**
     * 文本内容
     */
    private String content;

    /**
     * 工具名称（仅 tool_call 类型）
     */
    private String toolName;

    /**
     * 文件名（仅 file 类型）
     */
    private String fileName;

    /**
     * 文件下载 URL（仅 file 类型）
     */
    private String fileUrl;

    // ========== 静态工厂方法 ==========

    public static SseEvent thinking(String content) {
        return SseEvent.builder()
                .type("thinking")
                .content(content)
                .build();
    }

    public static SseEvent toolCall(String toolName, String content) {
        return SseEvent.builder()
                .type("tool_call")
                .toolName(toolName)
                .content(content)
                .build();
    }

    public static SseEvent file(String fileName, String fileUrl, String content) {
        return SseEvent.builder()
                .type("file")
                .fileName(fileName)
                .fileUrl(fileUrl)
                .content(content)
                .build();
    }

    public static SseEvent answer(String content) {
        return SseEvent.builder()
                .type("answer")
                .content(content)
                .build();
    }

    public static SseEvent error(String content) {
        return SseEvent.builder()
                .type("error")
                .content(content)
                .build();
    }

    public static SseEvent done() {
        return SseEvent.builder()
                .type("done")
                .build();
    }
}
