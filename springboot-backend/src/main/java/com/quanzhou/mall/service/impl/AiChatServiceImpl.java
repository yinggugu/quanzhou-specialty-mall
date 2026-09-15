package com.quanzhou.mall.service.impl;

import com.quanzhou.mall.bean.AiChatRecord;
import com.quanzhou.mall.bean.OllamaMessage;
import com.quanzhou.mall.bean.OllamaRequest;
import com.quanzhou.mall.mapper.AiChatRecordMapper;
import com.quanzhou.mall.service.AiChatService;
import com.quanzhou.mall.util.OllamaAiUtil;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AiChatServiceImpl implements AiChatService {

    @Resource
    private AiChatRecordMapper aiChatRecordMapper;

    @Resource
    private OllamaAiUtil ollamaAiUtil;

    @Value("${ollama.model:deepseek-r1:1.5b}")
    private String ollamaModel;

    // 商城导购固定系统提示词
    private static final String SYSTEM_PROMPT = "你是泉州特产商城专属AI导购，只解答商城商品、下单流程、支付、物流、售后退换货、优惠券活动相关问题。回答简洁亲切，口语化，不回答和商城无关内容；用户问无关内容礼貌引导咨询商品相关，禁止输出违规、敏感信息。";
    // 历史消息最大条数
    private static final int MAX_HISTORY = 20;

    @Override
    public String getUserSessionId(Integer userId) {
        // 查询用户是否已有会话记录
        List<AiChatRecord> exist = aiChatRecordMapper.selectLimitHistory(userId, null, 1);
        if (exist != null && !exist.isEmpty()) {
            return exist.get(0).getSessionId();
        }
        // 无记录，生成唯一sessionId
        String newSession = UUID.randomUUID().toString().replace("-", "");
        // 插入一条空占位记录绑定用户-session
        AiChatRecord init = new AiChatRecord();
        init.setUserId(userId);
        init.setSessionId(newSession);
        init.setRole("system");
        init.setContent("会话初始化");
        aiChatRecordMapper.insert(init);
        return newSession;
    }

    @Override
    public List<AiChatRecord> getLatestHistory(Integer userId, String sessionId) {
        return aiChatRecordMapper.selectLimitHistory(userId, sessionId, MAX_HISTORY);
    }

    @Override
    public void saveRecord(Integer userId, String sessionId, String role, String content) {
        AiChatRecord record = new AiChatRecord();
        record.setUserId(userId);
        record.setSessionId(sessionId);
        record.setRole(role);
        record.setContent(content);
        aiChatRecordMapper.insert(record);
    }

    @Override
    public String buildAiAnswer(Integer userId, String sessionId, String userContent) throws Exception {
        OllamaRequest req = new OllamaRequest();
        req.setModel(ollamaModel);
        List<OllamaMessage> msgList = new ArrayList<>();

        // 1. 插入系统导购提示词
        OllamaMessage sysMsg = new OllamaMessage();
        sysMsg.setRole("system");
        sysMsg.setContent(SYSTEM_PROMPT);
        msgList.add(sysMsg);

        // 2. 查询最多20条历史对话
        List<AiChatRecord> history = getLatestHistory(userId, sessionId);
        for (AiChatRecord r : history) {
            OllamaMessage m = new OllamaMessage();
            m.setRole(r.getRole());
            m.setContent(r.getContent());
            msgList.add(m);
        }

        // 3. 追加当前用户最新提问
        OllamaMessage userMsg = new OllamaMessage();
        userMsg.setRole("user");
        userMsg.setContent(userContent);
        msgList.add(userMsg);

        req.setMessages(msgList);

        // 4. 调用本地Ollama模型
        String aiReply = ollamaAiUtil.chat(req);
        return aiReply;
    }
}
