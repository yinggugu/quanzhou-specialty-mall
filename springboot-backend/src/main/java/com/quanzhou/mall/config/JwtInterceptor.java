package com.quanzhou.mall.config;

import com.quanzhou.mall.util.JwtUtil;
import com.quanzhou.mall.bean.Admin;
import com.quanzhou.mall.bean.User;
import com.quanzhou.mall.mapper.AdminMapper;
import com.quanzhou.mall.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT 认证拦截器 - 解析 Token，校验 type 字段，将用户信息放入 Request 属性
 *
 * ✅ 双表拆分改造：
 *    type=user  仅允许访问前台接口 (/api/chat, /api/user, /api/cart, /api/orders, /api/products...)
 *    type=admin 仅允许访问后台接口 (/api/admin/**)
 *    公开接口显式放行，其余 /api/** 必须携带有效 Token
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AdminMapper adminMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        String authHeader = request.getHeader("Authorization");

        if (isPublicEndpoint(request.getMethod(), path)) {
            return true;
        }

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeError(response, 401, "请先登录");
            return false;
        }

        if (authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (!jwtUtil.validateToken(token)) {
                writeError(response, 401, "Token 无效或已过期");
                return false;
            }

            String type = jwtUtil.getType(token);
            if (!"user".equals(type) && !"admin".equals(type)) {
                writeError(response, 401, "Token 身份无效");
                return false;
            }

            Integer userId = jwtUtil.getUserId(token);
            if (userId == null || !isActiveAccount(userId, type)) {
                writeError(response, 401, "账号不存在或已被禁用");
                return false;
            }

            // 将用户信息放入 Request 属性
            request.setAttribute("userId", userId);
            request.setAttribute("username", jwtUtil.getUsername(token));
            request.setAttribute("type", type); // ✅ type 替代 role

            // ✅ 权限校验：后台 /api/admin/** 仅放行 type=admin
            if (path.startsWith("/api/admin/")) {
                if (!"admin".equals(type)) {
                    writeError(response, 403, "无权限：仅管理员可访问");
                    return false;
                }
            }

            // 前台专属业务只接受普通用户 Token
            if (isUserOnlyPath(path) && "admin".equals(type)) {
                writeError(response, 403, "管理员请使用后台管理系统");
                return false;
            }

            return true;
        }

        return false;
    }

    private boolean isPublicEndpoint(String method, String path) {
        if (path.startsWith("/api/auth/")) return true;
        if ("GET".equalsIgnoreCase(method) &&
                (path.equals("/api/products") || path.startsWith("/api/products/"))) return true;
        return "GET".equalsIgnoreCase(method) && path.equals("/api/comment/list");
    }

    private boolean isUserOnlyPath(String path) {
        return path.startsWith("/api/user/")
                || path.equals("/api/cart") || path.startsWith("/api/cart/")
                || path.equals("/api/orders") || path.startsWith("/api/orders/")
                || path.equals("/api/afterSale") || path.startsWith("/api/afterSale/")
                || path.equals("/api/comment/add") || path.equals("/api/comment/like");
    }

    private void writeError(HttpServletResponse response, int status, String message) {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        try {
            response.getWriter().write("{\"code\":" + status + ",\"message\":\"" + message + "\",\"data\":null}");
        } catch (Exception ignored) {
        }
    }

    private boolean isActiveAccount(Integer id, String type) {
        if ("user".equals(type)) {
            User user = userMapper.selectById(id);
            return user != null && Integer.valueOf(1).equals(user.getStatus());
        }
        Admin admin = adminMapper.selectById(id);
        return admin != null && Integer.valueOf(1).equals(admin.getStatus());
    }
}
