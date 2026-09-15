package com.quanzhou.mall.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.quanzhou.mall.bean.AiChatRecord;
import com.quanzhou.mall.service.AiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AI 聊天 WebSocket 处理器
 * - 每个用户维护唯一 sessionId，持久化对话记录
 * - 调用本地 Ollama 模型生成回复
 * - 上下文最多携带 20 条历史消息
 */
@Component
public class AiChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(AiChatWebSocketHandler.class);

    @Autowired
    private AiChatService aiChatService;

    private final ObjectMapper mapper = new ObjectMapper();
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // userId → WebSocketSession 映射
    private static final ConcurrentHashMap<Integer, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Integer userId = (Integer) session.getAttributes().get("userId");
        if (userId == null) {
            try { session.close(); } catch (IOException ignored) {}
            return;
        }

        // 关闭旧连接（防止多Tab导致僵尸连接）
        WebSocketSession oldSession = userSessions.get(userId);
        if (oldSession != null && oldSession.isOpen()) {
            try {
                ObjectNode closeMsg = mapper.createObjectNode();
                closeMsg.put("type", "duplicateLogin");
                closeMsg.put("message", "您的账号在其他窗口打开了AI助手，当前窗口连接即将关闭");
                sendJson(oldSession, closeMsg);
                oldSession.close(CloseStatus.NORMAL);
            } catch (IOException ignored) {}
        }
        userSessions.put(userId, session);

        // 获取/创建用户唯一会话ID并推送给前端
        String userSession = aiChatService.getUserSessionId(userId);
        ObjectNode resp = mapper.createObjectNode();
        resp.put("type", "sessionCreated");
        resp.put("sessionId", userSession);
        sendJson(session, resp);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode json = mapper.readTree(message.getPayload());
        String type = json.has("type") ? json.get("type").asText() : "";
        Integer userId = (Integer) session.getAttributes().get("userId");
        if (userId == null) return;

        switch (type) {
            case "loadHistory":
                handleLoadHistory(session, json, userId);
                break;
            case "message":
                handleAiMessage(session, json, userId);
                break;
        }
    }

    /**
     * 加载历史消息
     */
    private void handleLoadHistory(WebSocketSession session, JsonNode json, Integer userId) {
        String sessionId = aiChatService.getUserSessionId(userId);
        if (!json.has("sessionId") || json.get("sessionId").asText().isEmpty()) {
            ObjectNode sidMsg = mapper.createObjectNode();
            sidMsg.put("type", "sessionCreated");
            sidMsg.put("sessionId", sessionId);
            sendJson(session, sidMsg);
        }

        List<AiChatRecord> history = aiChatService.getLatestHistory(userId, sessionId);
        ObjectNode resp = mapper.createObjectNode();
        resp.put("type", "historyMessages");

        ArrayNode messagesArr = mapper.createArrayNode();
        if (history != null && !history.isEmpty()) {
            for (AiChatRecord r : history) {
                ObjectNode msg = mapper.createObjectNode();
                msg.put("isAdmin", "assistant".equals(r.getRole()));
                msg.put("content", r.getContent() != null ? r.getContent() : "");
                msg.put("time", r.getCreateTime() != null ? r.getCreateTime().format(DTF) : "");
                messagesArr.add(msg);
            }
            resp.set("messages", messagesArr);
        } else {
            // 无历史，发送默认欢迎语
            ObjectNode welcome = mapper.createObjectNode();
            welcome.put("isAdmin", false);
            welcome.put("content", "您好！我是AI小助手，请问有什么可以帮您？");
            welcome.put("time", LocalDateTime.now().format(DTF));
            messagesArr.add(welcome);
            resp.set("messages", messagesArr);
        }
        sendJson(session, resp);
    }

    /**
     * 处理 AI 对话消息
     */
    private void handleAiMessage(WebSocketSession session, JsonNode json, Integer userId) {
        String sessionId = aiChatService.getUserSessionId(userId);
        String content = json.has("content") ? json.get("content").asText().trim() : "";

        if (content.isEmpty() || content.length() > 5000) return;

        if (!json.has("sessionId") || json.get("sessionId").asText().isEmpty() || "0".equals(json.get("sessionId").asText())) {
            ObjectNode sidMsg = mapper.createObjectNode();
            sidMsg.put("type", "sessionCreated");
            sidMsg.put("sessionId", sessionId);
            sendJson(session, sidMsg);
        }

        // 1. 保存用户消息入库
        aiChatService.saveRecord(userId, sessionId, "user", content);

        // 2. 调用 AI 获取回复
        String aiAnswer;
        try {
            aiAnswer = aiChatService.buildAiAnswer(userId, sessionId, content);
        } catch (Exception e) {
            aiAnswer = "AI导购暂时繁忙，可咨询人工客服";
        }

        // 3. 保存 AI 回复入库
        aiChatService.saveRecord(userId, sessionId, "assistant", aiAnswer);

        // 4. 推送 AI 回复给前端
        ObjectNode reply = mapper.createObjectNode();
        reply.put("type", "newMessage");
        reply.put("isAdmin", true);
        reply.put("content", aiAnswer);
        reply.put("time", LocalDateTime.now().format(DTF));
        sendJson(session, reply);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Integer userId = (Integer) session.getAttributes().get("userId");
        if (userId != null) {
            WebSocketSession existing = userSessions.get(userId);
            if (existing != null && existing.getId().equals(session.getId())) {
                userSessions.remove(userId);
            }
        }
    }

    /**
     * 发送 JSON 消息（带异常静默处理）
     */
    private void sendJson(WebSocketSession session, ObjectNode json) {
        try {
            if (session.isOpen()) {
                synchronized (session) {
                    session.sendMessage(new TextMessage(json.toString()));
                }
            }
        } catch (IOException e) {
            log.warn("AI WebSocket 消息发送失败: sessionId={}", session.getId(), e);
        }
    }
}
