package com.quanzhou.mall.controller;

import com.quanzhou.mall.config.ApiResponse;
import com.quanzhou.mall.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * 聊天 REST API - 历史消息加载、会话查询、文本文件存取
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    private static final String TEXT_DIR = "static/uploads/text/";
    private static final Path TEXT_BASE_DIR = Paths.get(TEXT_DIR).toAbsolutePath().normalize();

    /**
     * 保存长文本为bin文件，返回文件URL
     */
    @PostMapping("/text-save")
    public ApiResponse<?> textSave(@RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.isEmpty()) return ApiResponse.error("内容为空");
        try {
            Path dir = Paths.get(TEXT_DIR);
            if (!Files.exists(dir)) Files.createDirectories(dir);
            String name = UUID.randomUUID().toString() + ".bin";
            Files.write(dir.resolve(name), content.getBytes("UTF-8"));
            Map<String, Object> r = new HashMap<>();
            r.put("url", "/" + TEXT_DIR + name);
            return ApiResponse.ok("保存成功", r);
        } catch (IOException e) { return ApiResponse.error("保存失败"); }
    }

    /**
     * 读取bin文本文件
     */
    @GetMapping("/text-read")
    public ApiResponse<?> textRead(@RequestParam String url) {
        try {
            String relative = url.startsWith("/") ? url.substring(1) : url;
            Path file = Paths.get(relative).toAbsolutePath().normalize();
            if (!file.startsWith(TEXT_BASE_DIR) || !file.getFileName().toString().endsWith(".bin")) {
                return ApiResponse.error(403, "无权访问该文件");
            }
            byte[] bytes = Files.readAllBytes(file);
            Map<String, Object> r = new HashMap<>();
            r.put("content", new String(bytes, "UTF-8"));
            return ApiResponse.ok(r);
        } catch (IOException e) { return ApiResponse.error("读取失败"); }
    }

    /**
     * 获取当前用户的会话 ID
     */
    @GetMapping("/session")
    public ApiResponse<?> getSession(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        String username = (String) request.getAttribute("username");
        if (userId == null) return ApiResponse.error(401, "未登录");

        Integer[] result = chatService.getOrCreateSession(userId, username);
        Integer sessionId = result[0];
        if (sessionId == null) {
            return ApiResponse.error(500, "创建会话失败，请重试");
        }
        chatService.ensureWelcomeMessage(sessionId);

        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", sessionId);
        return ApiResponse.ok(data);
    }

    /**
     * 获取会话历史消息
     */
    @GetMapping("/messages/{sessionId}")
    public ApiResponse<?> getMessages(@PathVariable Integer sessionId, HttpServletRequest request) {
        if (sessionId == null || sessionId <= 0) {
            return ApiResponse.error(400, "无效的会话ID");
        }
        Integer userId = (Integer) request.getAttribute("userId");
        String type = (String) request.getAttribute("type");
        if (userId == null) return ApiResponse.error(401, "未登录");
        Integer ownerId = chatService.getSessionUserId(sessionId);
        if (ownerId == null) return ApiResponse.error(404, "会话不存在");
        if (!"admin".equals(type) && !ownerId.equals(userId)) return ApiResponse.error(403, "无权查看该会话");
        List<Map<String, Object>> messages = chatService.getMessages(sessionId);
        return ApiResponse.ok(messages);
    }

    /**
     * 管理员获取所有会话列表
     */
    @GetMapping("/sessions")
    public ApiResponse<?> getSessions(HttpServletRequest request) {
        // ✅ 仅管理员（type=admin）可访问会话列表
        String type = (String) request.getAttribute("type");
        if (!"admin".equals(type)) return ApiResponse.error(403, "无权限");
        List<Map<String, Object>> sessions = chatService.getAllSessions();
        return ApiResponse.ok(sessions);
    }
}
