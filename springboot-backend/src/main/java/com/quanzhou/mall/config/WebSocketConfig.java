package com.quanzhou.mall.config;

import com.quanzhou.mall.websocket.AiChatWebSocketHandler;
import com.quanzhou.mall.websocket.ChatWebSocketHandler;
import com.quanzhou.mall.websocket.WebSocketAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置 - 注册聊天处理器和握手鉴权拦截器
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @Autowired
    private AiChatWebSocketHandler aiChatWebSocketHandler;

    @Autowired
    private WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origins = java.util.Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
        // 人工客服聊天
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOrigins(origins);

        // AI 小助手聊天（复用同一个鉴权拦截器）
        registry.addHandler(aiChatWebSocketHandler, "/ws/ai-chat")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOrigins(origins);
    }
}
