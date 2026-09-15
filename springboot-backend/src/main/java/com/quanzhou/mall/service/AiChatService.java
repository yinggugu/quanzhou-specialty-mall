package com.quanzhou.mall.service;

import com.quanzhou.mall.bean.AiChatRecord;
import java.util.List;

public interface AiChatService {
    // 获取用户固定sessionId，不存在则生成UUID存入数据库
    String getUserSessionId(Integer userId);
    // 查询用户会话最近N条历史对话（最多20条）
    List<AiChatRecord> getLatestHistory(Integer userId, String sessionId);
    // 保存单条对话记录
    void saveRecord(Integer userId, String sessionId, String role, String content);
    // 组装完整请求上下文：系统提示词 + 20条历史 + 当前提问，调用Ollama并返回AI回复
    String buildAiAnswer(Integer userId, String sessionId, String userContent) throws Exception;
}
