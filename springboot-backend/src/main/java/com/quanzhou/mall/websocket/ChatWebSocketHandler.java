package com.quanzhou.mall.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.quanzhou.mall.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
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
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 聊天 WebSocket 核心处理器
 * - 维护 userId → WebSocketSession 映射（普通用户）
 * - 维护所有管理员 session 集合，用于广播
 * - 处理消息收发、会话管理、已读标记
 *
 * 【字段语义说明 - ✅ 修复8】
 * chat_message.is_admin : 0=普通用户发送, 1=管理员/客服发送
 * chat_message.user_id   : 消息发送方的用户ID
 *   - 普通用户发消息时 = 用户自己的ID
 *   - 管理员发消息时   = 管理员的ID（记录是哪位客服回复的）
 * 前后端映射：数据库 is_admin (INT 0/1) ↔ 前端JSON isAdmin (Boolean false/true)
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    @Autowired
    private ChatService chatService;

    // ✅ 修复7：Spring 内置线程池，替代 new Thread()
    @Autowired
    @Qualifier("chatTaskExecutor")
    private ThreadPoolTaskExecutor chatTaskExecutor;

    private final ObjectMapper mapper = new ObjectMapper();
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // userId → WebSocketSession (用户端连接)
    private static final ConcurrentHashMap<Integer, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    // 管理员 WebSocketSession 集合
    private static final CopyOnWriteArraySet<WebSocketSession> adminSessions = new CopyOnWriteArraySet<>();
    // WebSocketSession.id → userId 反向映射（用于快速查找发送方身份）
    private static final ConcurrentHashMap<String, Integer> sessionUserMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Integer userId = (Integer) session.getAttributes().get("userId");
        Integer role = (Integer) session.getAttributes().get("role");
        boolean isAdmin = role != null && role == 1;

        if (userId == null) {
            try { session.close(); } catch (IOException ignored) {}
            return;
        }
        sessionUserMap.put(session.getId(), userId);

        if (role != null && role == 1) {
            // --- 管理员连接 ---
            adminSessions.add(session);
            pushSessionListToAdmin(session);
        } else {
            // --- 用户连接 ---
            // ✅ 修复6：新Tab连接时，优雅关闭旧的WebSocket连接，杜绝僵尸连接
            WebSocketSession oldSession = userSessions.get(userId);
            if (oldSession != null && oldSession.isOpen()) {
                try {
                    ObjectNode closeMsg = mapper.createObjectNode();
                    closeMsg.put("type", "duplicateLogin");
                    closeMsg.put("message", "您的账号在其他窗口打开了聊天，当前窗口连接即将关闭");
                    sendJson(oldSession, closeMsg);
                    oldSession.close(CloseStatus.NORMAL);
                } catch (IOException ignored) {}
            }
            userSessions.put(userId, session);

            String username = (String) session.getAttributes().get("username");
            if (username == null) username = "用户" + userId;
            Integer[] sessionResult = chatService.getOrCreateSession(userId, username);
            Integer sessionId = sessionResult[0];
            chatService.ensureWelcomeMessage(sessionId);

            // 通知用户其会话ID
            ObjectNode msg = mapper.createObjectNode();
            msg.put("type", "sessionCreated");
            msg.put("sessionId", sessionId);
            sendJson(session, msg);

            // 推送历史消息（含欢迎语）— 用户侧不过滤
            pushHistory(session, sessionId, false);

            // 通知管理员会话列表更新
            broadcastSessionListToAdmins();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode json = mapper.readTree(message.getPayload());
        String type = json.has("type") ? json.get("type").asText() : "message";
        Integer userId = sessionUserMap.get(session.getId());
        Integer role = (Integer) session.getAttributes().get("role");
        boolean isAdmin = role != null && role == 1;

        // ✅ 修复2：userId 判空兜底 — sessionUserMap 取不到 userId 时，尝试从 session attributes 获取
        if (userId == null) {
            userId = (Integer) session.getAttributes().get("userId");
        }

        switch (type) {
            case "message":
                handleChatMessage(session, json, userId, role);
                break;
            case "markRead":
                if (isAdmin) handleMarkRead(json); else sendError(session, "无权执行该操作");
                break;
            case "updateSession":
                if (isAdmin) handleUpdateSession(json); else sendError(session, "无权执行该操作");
                break;
            case "loadSessions":
                if (isAdmin) pushSessionListToAdmin(session); else sendError(session, "无权查看会话列表");
                break;
            case "loadHistory":
                Integer sid = json.has("sessionId") ? json.get("sessionId").asInt() : null;
                if (sid != null && (isAdmin || userId.equals(chatService.getSessionUserId(sid)))) {
                    pushHistory(session, sid, isAdmin);
                } else {
                    sendError(session, "无权查看该会话");
                }
                break;
        }
    }

    /**
     * 处理聊天消息 - 入库并转发
     */
    private void handleChatMessage(WebSocketSession session, JsonNode json, Integer userId, Integer role) {
        try {
            Integer sessionId = json.has("sessionId") ? json.get("sessionId").asInt() : null;
            boolean isAdmin = (role != null && role == 1);

            // ================================================================
            // ✅ 修复1：管理员发消息前置校验 sessionId 合法性
            // ================================================================
            if ((sessionId == null || sessionId <= 0) && isAdmin) {
                ObjectNode err = mapper.createObjectNode();
                err.put("type", "error");
                err.put("message", "请先选择要回复的会话");
                sendJson(session, err);
                return;
            }

            // 普通用户未携带 sessionId → 自动获取或创建会话
            if ((sessionId == null || sessionId <= 0) && !isAdmin) {
                String username = (String) session.getAttributes().get("username");
                Integer[] result = chatService.getOrCreateSession(userId, username);
                sessionId = result[0];
                chatService.ensureWelcomeMessage(sessionId);

                ObjectNode sidMsg = mapper.createObjectNode();
                sidMsg.put("type", "sessionCreated");
                sidMsg.put("sessionId", sessionId);
                sendJson(session, sidMsg);
                pushHistory(session, sessionId, false);
            }

            Integer ownerId = chatService.getSessionUserId(sessionId);
            if (ownerId == null || (!isAdmin && !ownerId.equals(userId))) {
                sendError(session, "无权向该会话发送消息");
                return;
            }

            // ================================================================
            // ✅ 修复2：userId 二次兜底 — 如果仍然为空，从 session 属性获取后再判空拦截
            // ================================================================
            if (userId == null) {
                userId = (Integer) session.getAttributes().get("userId");
                if (userId == null) {
                    // 无法确定发送方身份，丢弃异常消息
                    return;
                }
            }

            String msgType = json.has("msgType") ? json.get("msgType").asText() : "text";
            String content = json.has("content") ? json.get("content").asText() : "";
            if (content.trim().isEmpty() || content.length() > 5000) {
                sendError(session, "消息内容为空或超过5000个字符");
                return;
            }
            String orderInfo = json.has("orderInfo") ? json.get("orderInfo").toString() : null;
            String contentFileUrl = json.has("contentFileUrl") ? json.get("contentFileUrl").asText() : null;

            // ================================================================
            // ✅ 修复7：催发货自动回复 — 使用 Spring 线程池替代 new Thread()
            // ================================================================
            String autoReply = chatService.checkAutoReply(content);
            if (autoReply != null && !isAdmin) {
                final Integer fSessionId = sessionId;
                final Integer fUserId = userId;
                chatTaskExecutor.execute(() -> {
                    try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                    int replyId = chatService.saveMessage(fSessionId, 0, 1, "text", autoReply, null);
                    ObjectNode reply = mapper.createObjectNode();
                    reply.put("type", "newMessage");
                    reply.put("msgId", replyId);
                    reply.put("sessionId", fSessionId);
                    reply.put("userId", 0);           // 系统自动回复，userId=0
                    reply.put("isAdmin", true);       // ✅ 以客服身份发送
                    reply.put("msgType", "text");
                    reply.put("content", autoReply);
                    reply.put("time", LocalDateTime.now().format(DTF));

                    // 推送给目标用户
                    WebSocketSession userWs = userSessions.get(fUserId);
                    if (userWs != null && userWs.isOpen()) {
                        sendJson(userWs, reply);
                    }
                    // 广播给所有管理员
                    for (WebSocketSession adminWs : adminSessions) {
                        if (adminWs.isOpen()) sendJson(adminWs, reply);
                    }
                    broadcastSessionListToAdmins();
                });
            }

            // ================================================================
            // 消息入库（✅ 修复8：is_admin 标识准确，0=用户 1=管理员）
            // ================================================================
            int isAdminFlag = isAdmin ? 1 : 0;
            int msgId = chatService.saveMessage(sessionId, userId, isAdminFlag, msgType, content, orderInfo, contentFileUrl);

            // 构建推送 JSON 载荷
            ObjectNode push = mapper.createObjectNode();
            push.put("type", "newMessage");
            push.put("msgId", msgId);
            push.put("sessionId", sessionId);
            push.put("userId", userId);
            push.put("isAdmin", isAdmin);            // ✅ 前端Bug1修复：Boolean 类型，false=用户 true=客服
            push.put("msgType", msgType);
            push.put("content", content);
            if (orderInfo != null) {
                push.put("orderInfo", mapper.readTree(orderInfo));
            }
            push.put("time", LocalDateTime.now().format(DTF));

            // ================================================================
            // ✅ 前端Bug2修复：推送消息时跳过发送方自己的WebSocket连接，避免
            //    前端乐观渲染 + 服务端回显 = 消息重复渲染两遍
            // ================================================================

            // 推送给会话所属用户（跳过发送方自己的连接，防止回显重复）
            Integer sessionOwnerId = chatService.getSessionUserId(sessionId);
            if (sessionOwnerId != null) {
                WebSocketSession userWs = userSessions.get(sessionOwnerId);
                if (userWs != null && userWs.isOpen() && !userWs.getId().equals(session.getId())) {
                    sendJson(userWs, push);
                } else if (userWs == null || !userWs.isOpen()) {
                    // 用户离线，增加未读计数
                    chatService.incrementUnread(sessionId);
                }
            }

            // 广播给所有管理员（跳过发送方自己的连接）
            String senderUsername = (String) session.getAttributes().get("username");
            if (senderUsername == null) {
                senderUsername = chatService.getSessionUsername(sessionId);
            }
            for (WebSocketSession adminWs : adminSessions) {
                if (adminWs.isOpen() && !adminWs.getId().equals(session.getId())) {
                    ObjectNode adminPush = push.deepCopy();
                    adminPush.put("username", senderUsername != null ? senderUsername : "未知用户");
                    sendJson(adminWs, adminPush);
                }
            }

            // 推送更新后的会话列表给管理员
            broadcastSessionListToAdmins();

        } catch (Exception e) {
            log.warn("聊天消息处理失败: sessionId={}", session.getId(), e);
        }
    }

    /**
     * 标记已读
     */
    private void handleMarkRead(JsonNode json) {
        if (!json.has("sessionId")) return;
        int sessionId = json.get("sessionId").asInt();
        chatService.markRead(sessionId);
        broadcastSessionListToAdmins();
    }

    /**
     * 更新会话信息（备注、咨询类型、状态）
     */
    private void handleUpdateSession(JsonNode json) {
        try {
            int sessionId = json.get("sessionId").asInt();
            String remark = json.has("remark") ? json.get("remark").asText() : null;
            String consultType = json.has("consultType") ? json.get("consultType").asText() : null;
            String status = json.has("status") ? json.get("status").asText() : null;

            chatService.updateSession(sessionId, remark, consultType, status);

            // 通知关联用户
            Integer ownerId = chatService.getSessionUserId(sessionId);
            WebSocketSession userWs = userSessions.get(ownerId);
            if (userWs != null && userWs.isOpen()) {
                ObjectNode push = mapper.createObjectNode();
                push.put("type", "sessionUpdated");
                push.put("sessionId", sessionId);
                if (status != null) push.put("status", status);
                sendJson(userWs, push);
            }

            broadcastSessionListToAdmins();
        } catch (Exception e) {
            log.warn("会话更新失败", e);
        }
    }

    /**
     * 推送会话列表给指定管理员
     */
    private void pushSessionListToAdmin(WebSocketSession adminSession) {
        try {
            List<Map<String, Object>> sessions = chatService.getAllSessions();
            ObjectNode msg = mapper.createObjectNode();
            msg.put("type", "sessionList");
            msg.putPOJO("sessions", sessions);
            sendJson(adminSession, msg);
        } catch (Exception e) {
            log.warn("管理员会话列表推送失败: sessionId={}", adminSession.getId(), e);
        }
    }

    /**
     * 广播会话列表给所有管理员
     */
    private void broadcastSessionListToAdmins() {
        for (WebSocketSession adminWs : adminSessions) {
            if (adminWs.isOpen()) {
                pushSessionListToAdmin(adminWs);
            }
        }
    }

    /**
     * 推送历史消息
     * @param isAdmin true=管理员视图（过滤已软删除消息），false=用户视图（展示全部）
     */
    private void pushHistory(WebSocketSession session, int sessionId, boolean isAdmin) {
        try {
            // ✅ 管理员侧过滤 is_del=1 的软删除消息，用户侧展示全部
            List<Map<String, Object>> messages = chatService.getMessages(sessionId, isAdmin);
            ObjectNode msg = mapper.createObjectNode();
            msg.put("type", "historyMessages");
            msg.put("sessionId", sessionId);
            msg.putPOJO("messages", messages);
            sendJson(session, msg);
        } catch (Exception e) {
            log.warn("历史消息推送失败: sessionId={}", session.getId(), e);
        }
    }

    /**
     * 发送 JSON 消息（带异常静默处理）
     */
    private void sendJson(WebSocketSession session, ObjectNode json) {
        try {
            if (session.isOpen()) {
                synchronized (session) { // WebSocket 会话级别的同步，避免并发写入
                    session.sendMessage(new TextMessage(json.toString()));
                }
            }
        } catch (IOException e) {
            log.warn("WebSocket 消息发送失败: sessionId={}", session.getId(), e);
        }
    }

    private void sendError(WebSocketSession session, String message) {
        ObjectNode error = mapper.createObjectNode();
        error.put("type", "error");
        error.put("message", message);
        sendJson(session, error);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Integer userId = sessionUserMap.remove(session.getId());
        if (userId != null) {
            WebSocketSession existing = userSessions.get(userId);
            // 仅当 userSessions 中存的确实是当前关闭的 session 时才移除
            // （防止新Tab的session被误删）
            if (existing != null && existing.getId().equals(session.getId())) {
                userSessions.remove(userId);
            }
        }
        adminSessions.remove(session);
    }
}
