package com.jiege.jieaiagent.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jiege.jieaiagent.mapper.ChatMessageMapper;
import com.jiege.jieaiagent.mapper.ConversationMapper;
import com.jiege.jieaiagent.model.ChatMessage;
import com.jiege.jieaiagent.model.Conversation;
import com.jiege.jieaiagent.model.User;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    @Resource
    private ConversationMapper conversationMapper;

    @Resource
    private ChatMessageMapper chatMessageMapper;

    @GetMapping
    public Map<String, Object> listConversations(
            HttpServletRequest request,
            @RequestParam(defaultValue = "love_app") String appType) {
        User user = (User) request.getAttribute("currentUser");

        List<Conversation> conversations = conversationMapper.selectList(
            new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserId, user.getId())
                .eq(Conversation::getAppType, appType)
                .orderByDesc(Conversation::getUpdatedAt));

        List<Map<String, Object>> list = conversations.stream().map(c -> {
            String lastMsg = "";
            List<ChatMessage> msgs = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getConversationId, c.getId())
                    .orderByDesc(ChatMessage::getTimestamp)
                    .last("LIMIT 1"));
            if (!msgs.isEmpty()) {
                lastMsg = msgs.get(0).getContent();
                if (lastMsg != null && lastMsg.length() > 50) {
                    lastMsg = lastMsg.substring(0, 50) + "...";
                }
            }
            return Map.<String, Object>of(
                "id", c.getId(),
                "title", c.getTitle(),
                "appType", c.getAppType(),
                "lastMessage", lastMsg,
                "createdAt", c.getCreatedAt().toString(),
                "updatedAt", c.getUpdatedAt().toString()
            );
        }).collect(Collectors.toList());

        return Map.of("code", 0, "data", list);
    }

    @PostMapping
    public Map<String, Object> createConversation(
            HttpServletRequest request,
            @RequestBody Map<String, String> body) {
        User user = (User) request.getAttribute("currentUser");
        String appType = body.getOrDefault("appType", "love_app");
        String title = body.getOrDefault("title", "新对话");

        String id = UUID.randomUUID().toString().replace("-", "");
        Conversation conv = Conversation.builder()
            .id(id)
            .userId(user.getId())
            .title(title)
            .appType(appType)
            .build();
        conversationMapper.insert(conv);

        return Map.of("code", 0, "data", Map.of(
            "id", id, "title", title, "appType", appType
        ));
    }

    @GetMapping("/{id}/messages")
    public Map<String, Object> getMessages(
            HttpServletRequest request,
            @PathVariable String id) {
        User user = (User) request.getAttribute("currentUser");

        Conversation conv = conversationMapper.selectOne(
            new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getId, id)
                .eq(Conversation::getUserId, user.getId()));
        if (conv == null) {
            return Map.of("code", 403, "message", "无权访问该会话");
        }

        List<ChatMessage> messages = chatMessageMapper.selectList(
            new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, id)
                .orderByAsc(ChatMessage::getTimestamp));

        List<Map<String, Object>> msgList = messages.stream().map(m -> Map.<String, Object>of(
            "content", m.getContent(),
            "role", switch (m.getType()) {
                case "USER" -> "user";
                default -> "assistant";
            },
            "timestamp", m.getTimestamp() != null
                ? m.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                : System.currentTimeMillis()
        )).collect(Collectors.toList());

        return Map.of("code", 0, "data", Map.of(
            "conversation", Map.of(
                "id", conv.getId(),
                "title", conv.getTitle(),
                "appType", conv.getAppType()
            ),
            "messages", msgList
        ));
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteConversation(
            HttpServletRequest request,
            @PathVariable String id) {
        User user = (User) request.getAttribute("currentUser");

        conversationMapper.delete(
            new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getId, id)
                .eq(Conversation::getUserId, user.getId()));
        chatMessageMapper.delete(
            new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, id));

        return Map.of("code", 0, "message", "已删除");
    }

    @PutMapping("/{id}/title")
    public Map<String, Object> updateTitle(
            HttpServletRequest request,
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        User user = (User) request.getAttribute("currentUser");

        Conversation conv = conversationMapper.selectById(id);
        if (conv != null && conv.getUserId().equals(user.getId())) {
            conv.setTitle(body.get("title"));
            conversationMapper.updateById(conv);
        }

        return Map.of("code", 0, "message", "已更新");
    }
}
