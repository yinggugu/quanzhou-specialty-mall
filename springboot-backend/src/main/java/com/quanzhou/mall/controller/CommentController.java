package com.quanzhou.mall.controller;

import com.quanzhou.mall.config.ApiResponse;
import com.quanzhou.mall.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 评价接口（前台用户）
 */
@RestController
@RequestMapping("/api/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    /**
     * POST /api/comment/add — 提交评价
     */
    @PostMapping("/add")
    public ApiResponse<?> add(@RequestBody Map<String, Object> body,
                              @RequestAttribute(name = "userId", required = false) Integer userId) {
        if (userId == null) return ApiResponse.error(401, "请先登录");
        Map<String, Object> res = commentService.addComment(
            userId,
            toInt(body.get("productId")),
            toInt(body.get("orderItemId")),
            toInt(body.get("score")),
            (String) body.get("content"),
            (String) body.get("imgList"),
            toInt(body.get("isAnonymous"))
        );
        int code = (int) res.get("code");
        if (code == 200) return ApiResponse.ok(res.get("msg"));
        return ApiResponse.error(code, (String) res.get("msg"));
    }

    /**
     * GET /api/comment/list — 商品评价列表
     */
    @GetMapping("/list")
    public ApiResponse<?> list(@RequestParam Integer productId,
                               @RequestParam(defaultValue = "all") String filterType,
                               @RequestParam(defaultValue = "newest") String sortType,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int pageSize) {
        Map<String, Object> data = commentService.getCommentList(productId, filterType, sortType, page, pageSize);
        return ApiResponse.ok(data);
    }

    /**
     * POST /api/comment/like — 点赞
     */
    @PostMapping("/like")
    public ApiResponse<?> like(@RequestBody Map<String, Object> body) {
        commentService.likeComment(toInt(body.get("commentId")));
        return ApiResponse.ok("点赞成功");
    }

    private int toInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).intValue();
        return Integer.parseInt(obj.toString());
    }
}
