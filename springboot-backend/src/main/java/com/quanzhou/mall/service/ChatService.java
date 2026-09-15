package com.quanzhou.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quanzhou.mall.bean.*;
import com.quanzhou.mall.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ChatService {

    @Autowired private ChatSessionMapper sessionMapper;
    @Autowired private ChatMessageMapper messageMapper;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public Integer[] getOrCreateSession(Integer userId, String username) {
        Integer sessionId = sessionMapper.findActiveByUser(userId);
        if (sessionId != null) return new Integer[]{sessionId, 0};
        ChatSession cs = new ChatSession();
        cs.setUserId(userId); cs.setUsername(username != null ? username : "用户"+userId);
        cs.setConsultType("其他问题"); cs.setStatus("进行中");
        sessionMapper.insert(cs);
        return new Integer[]{cs.getId(), 1};
    }

    public void ensureWelcomeMessage(int sessionId) {
        if (messageMapper.countBySession(sessionId) == 0) {
            ChatMessage cm = new ChatMessage();
            cm.setSessionId(sessionId); cm.setUserId(0); cm.setIsAdmin(1); cm.setMsgType("text");
            cm.setContent("您好！欢迎咨询泉州特产商城客服，请问有什么可以帮您？");
            messageMapper.insert(cm);
        }
    }

    public String checkAutoReply(String content) {
        if (content != null && content.contains("催促") && content.contains("发货"))
            return "已收到您的催促，我们会尽快为您处理发货事宜，请您耐心等待~";
        return null;
    }

    public int saveMessage(Integer sessionId, Integer userId, int isAdmin, String msgType, String content, String orderInfo, String contentFileUrl) {
        ChatMessage cm = new ChatMessage();
        cm.setSessionId(sessionId); cm.setUserId(userId); cm.setIsAdmin(isAdmin);
        cm.setMsgType(msgType); cm.setContent(content); cm.setOrderInfo(orderInfo != null ? orderInfo : "");
        cm.setContentFileUrl(contentFileUrl != null ? contentFileUrl : "");
        messageMapper.insert(cm);
        String lastMsg = "text".equals(msgType) ? (content.length() > 50 ? content.substring(0, 50) + "..." : content) : "[订单卡片]";
        sessionMapper.updateLastMsg(sessionId, lastMsg);
        return cm.getId();
    }

    public int saveMessage(Integer sessionId, Integer userId, int isAdmin, String msgType, String content, String orderInfo) {
        return saveMessage(sessionId, userId, isAdmin, msgType, content, orderInfo, null);
    }

    public List<Map<String, Object>> getMessages(int sessionId, boolean filterDeleted) {
        List<ChatMessage> list = messageMapper.findBySession(sessionId, filterDeleted);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatMessage cm : list) {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("id", cm.getId()); msg.put("sessionId", cm.getSessionId()); msg.put("userId", cm.getUserId());
            msg.put("isAdmin", cm.getIsAdmin() != null && cm.getIsAdmin() == 1);
            msg.put("msgType", cm.getMsgType()); msg.put("content", cm.getContent());
            msg.put("contentFileUrl", cm.getContentFileUrl());
            String oi = cm.getOrderInfo();
            if (oi != null && !oi.isEmpty()) {
                try { msg.put("orderInfo", new com.fasterxml.jackson.databind.ObjectMapper().readTree(oi)); } catch (Exception ignored) {}
            }
            msg.put("time", cm.getCreateTime());
            result.add(msg);
        }
        return result;
    }

    public List<Map<String, Object>> getMessages(int sessionId) { return getMessages(sessionId, false); }

    public List<Map<String, Object>> getAllSessions() {
        List<ChatSession> list = sessionMapper.selectList(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getAdminDel, 0)
                .last("ORDER BY FIELD(status,'待回复','进行中','已完成'), last_time DESC"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatSession cs : list) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("id", cs.getId()); s.put("userId", cs.getUserId()); s.put("username", cs.getUsername());
            s.put("desc", cs.getLastMessage() != null ? cs.getLastMessage() : "");
            s.put("unread", cs.getUnreadCount() != null ? cs.getUnreadCount() : 0);
            s.put("orderNo", cs.getOrderNo() != null ? cs.getOrderNo() : "");
            s.put("goodsName", cs.getGoodsName() != null ? cs.getGoodsName() : "");
            s.put("consultType", cs.getConsultType()); s.put("status", cs.getStatus());
            s.put("remark", cs.getRemark() != null ? cs.getRemark() : "");
            s.put("lastTime", cs.getLastTime() != null ? cs.getLastTime() : "");
            result.add(s);
        }
        return result;
    }

    public void markRead(int sessionId) { sessionMapper.markRead(sessionId); }
    public void incrementUnread(int sessionId) { sessionMapper.incrUnread(sessionId); }
    public void updateSession(int sessionId, String remark, String consultType, String status) {
        ChatSession cs = sessionMapper.selectById(sessionId);
        if (cs != null) {
            if (remark != null) cs.setRemark(remark);
            if (consultType != null) cs.setConsultType(consultType);
            if (status != null) cs.setStatus(status);
            sessionMapper.updateById(cs);
        }
    }
    public Integer getSessionUserId(int sessionId) { return sessionMapper.getUserId(sessionId); }
    public String getSessionUsername(int sessionId) { return sessionMapper.getUsername(sessionId); }
    public void markUnread(int sessionId) { sessionMapper.markUnread(sessionId); }
    public void softDeleteSessionMessages(int sessionId) { messageMapper.softDeleteBySession(sessionId); sessionMapper.adminDel(sessionId); }
}
