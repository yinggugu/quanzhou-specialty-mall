package com.quanzhou.mall.websocket;

import com.quanzhou.mall.util.JwtUtil;
import com.quanzhou.mall.bean.Admin;
import com.quanzhou.mall.bean.User;
import com.quanzhou.mall.mapper.AdminMapper;
import com.quanzhou.mall.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手鉴权拦截器 - 从 query 参数提取 JWT，验证后将 userId/username/type 存入 attributes
 *
 * ✅ 双表拆分改造：用 type 字段（"user"/"admin"）替代旧 role 字段
 *    type=user  → 作为普通用户建立连接，发消息 is_admin=0
 *    type=admin → 作为管理员建立连接，发消息 is_admin=1
 */
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AdminMapper adminMapper;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            String token = servletRequest.getServletRequest().getParameter("token");
            if (token != null && jwtUtil.validateToken(token)) {
                Claims claims = jwtUtil.parseToken(token);

                // userId — 保持 Integer 类型
                Object userIdObj = claims.get("userId");
                if (userIdObj instanceof Integer) {
                    attributes.put("userId", userIdObj);
                } else if (userIdObj instanceof Number) {
                    attributes.put("userId", ((Number) userIdObj).intValue());
                }

                // username
                attributes.put("username", claims.get("username"));

                // ✅ type 字段（"user" 或 "admin"）替代旧 role 字段
                Object typeObj = claims.get("type");
                String type = null;
                if (typeObj instanceof String) {
                    type = (String) typeObj;
                }
                Integer userId = (Integer) attributes.get("userId");
                if (userId == null || !isActiveAccount(userId, type)) return false;
                if (request.getURI().getPath().endsWith("/ws/ai-chat") && !"user".equals(type)) return false;
                attributes.put("type", type);

                // ✅ 兼容旧代码：同时设置 role（管理员=1，用户=0）
                attributes.put("role", "admin".equals(type) ? 1 : 0);

                return true;
            }
        }
        return false;
    }

    private boolean isActiveAccount(Integer id, String type) {
        if ("user".equals(type)) {
            User user = userMapper.selectById(id);
            return user != null && Integer.valueOf(1).equals(user.getStatus());
        }
        if ("admin".equals(type)) {
            Admin admin = adminMapper.selectById(id);
            return admin != null && Integer.valueOf(1).equals(admin.getStatus());
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
