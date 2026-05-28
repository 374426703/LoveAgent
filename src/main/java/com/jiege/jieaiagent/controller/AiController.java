package com.jiege.jieaiagent.controller;

import com.jiege.jieaiagent.agent.JieAgent;
import com.jiege.jieaiagent.app.LoveApp;
import com.jiege.jieaiagent.mapper.ConversationMapper;
import com.jiege.jieaiagent.model.Conversation;
import com.jiege.jieaiagent.model.User;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

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
}
