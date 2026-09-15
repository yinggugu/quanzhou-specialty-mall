package com.quanzhou.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quanzhou.mall.bean.Comment;
import com.quanzhou.mall.bean.Product;
import com.quanzhou.mall.bean.User;
import com.quanzhou.mall.mapper.CommentMapper;
import com.quanzhou.mall.mapper.ProductMapper;
import com.quanzhou.mall.mapper.UserMapper;
import com.quanzhou.mall.mapper.WordFilterMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class CommentService {

    @Autowired private CommentMapper commentMapper;
    @Autowired private WordFilterMapper wordFilterMapper;
    @Autowired private ProductMapper productMapper;
    @Autowired private UserMapper userMapper;

    public Map<String, Object> addComment(Integer userId, Integer productId, Integer orderItemId,
                                           Integer score, String content, String imgList, Integer isAnonymous) {
        Map<String, Object> r = new HashMap<>();
        if (commentMapper.selectOne(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getUserId, userId).eq(Comment::getOrderItemId, orderItemId)) != null) {
            r.put("code", 400); r.put("msg", "该商品已完成评价，无法重复发布"); return r;
        }
        String filtered = filterSensitiveWords(content);
        Comment c = new Comment();
        c.setProductId(productId); c.setUserId(userId); c.setOrderItemId(orderItemId);
        c.setScore(score); c.setContent(filtered != null ? filtered : "");
        c.setImgList(imgList != null ? imgList : ""); c.setIsAnonymous(isAnonymous != null ? isAnonymous : 0);
        commentMapper.insert(c);
        r.put("code", 200); r.put("msg", "评价成功"); return r;
    }

    private String filterSensitiveWords(String text) {
        if (text == null || text.isEmpty()) return text;
        for (String w : wordFilterMapper.getAllWords()) {
            if (text.contains(w)) text = text.replace(w, "***");
        }
        return text;
    }

    public Map<String, Object> getCommentList(Integer productId, String filterType, String sortType, int page, int pageSize) {
        LambdaQueryWrapper<Comment> qw = new LambdaQueryWrapper<>();
        qw.eq(Comment::getProductId, productId).eq(Comment::getIsShow, 1);
        if ("image".equals(filterType)) qw.ne(Comment::getImgList, "");
        if ("good".equals(sortType)) qw.orderByDesc(Comment::getScore);
        else if ("bad".equals(sortType)) qw.orderByAsc(Comment::getScore);
        qw.orderByDesc(Comment::getCreateTime);
        Page<Comment> p = commentMapper.selectPage(new Page<>(page, pageSize), qw);
        // 统计
        int total = (int) p.getTotal();
        double avgScore = 0;
        int goodCount = 0;
        List<Comment> all = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getProductId, productId).eq(Comment::getIsShow, 1));
        for (Comment c : all) { avgScore += c.getScore(); if (c.getScore() >= 4) goodCount++; }
        avgScore = all.isEmpty() ? 0 : Math.round(avgScore / all.size() * 10.0) / 10.0;
        int goodRate = total > 0 ? goodCount * 100 / total : 0;
        Map<String, Object> r = new HashMap<>();
        r.put("total", total); r.put("avgScore", avgScore); r.put("goodRate", goodRate);
        r.put("list", enrichComments(p.getRecords())); return r;
    }

    public void likeComment(int commentId) {
        Comment c = commentMapper.selectById(commentId);
        if (c != null) { c.setLikeCount(c.getLikeCount() != null ? c.getLikeCount() + 1 : 1); commentMapper.updateById(c); }
    }

    public Map<String, Object> adminCommentList(String goodsName, String scoreFilter, int page, int pageSize) {
        LambdaQueryWrapper<Comment> qw = new LambdaQueryWrapper<>();
        // 按商品名称搜索：先查匹配的商品ID列表
        if (goodsName != null && !goodsName.trim().isEmpty()) {
            List<Product> matchedProducts = productMapper.selectList(
                new LambdaQueryWrapper<Product>().like(Product::getName, goodsName.trim()));
            if (!matchedProducts.isEmpty()) {
                List<Integer> productIds = new ArrayList<>();
                for (Product p : matchedProducts) productIds.add(p.getId());
                qw.in(Comment::getProductId, productIds);
            } else {
                qw.eq(Comment::getProductId, -1); // 无匹配商品，返回空
            }
        }
        if ("good".equals(scoreFilter)) qw.ge(Comment::getScore, 4);
        else if ("mid".equals(scoreFilter)) qw.eq(Comment::getScore, 3);
        else if ("bad".equals(scoreFilter)) qw.le(Comment::getScore, 2);
        qw.orderByDesc(Comment::getCreateTime);
        Page<Comment> p = commentMapper.selectPage(new Page<>(page, pageSize), qw);
        Map<String, Object> r = new HashMap<>();
        r.put("total", (int) p.getTotal());
        r.put("list", enrichComments(p.getRecords()));
        return r;
    }

    public void hideComment(int id) {
        Comment c = commentMapper.selectById(id);
        if (c != null) { c.setIsShow(0); commentMapper.updateById(c); }
    }

    public void replyComment(int id, String reply) {
        Comment c = commentMapper.selectById(id);
        if (c != null) { c.setReplyContent(reply); commentMapper.updateById(c); }
    }

    public void addWord(String word) { wordFilterMapper.addWord(word); }
    public List<String> getWords() { return wordFilterMapper.getAllWords(); }
    public void deleteWord(int id) { wordFilterMapper.deleteWord(id); }
    public void updateWord(int id, String word) { wordFilterMapper.updateWord(id, word); }

    public boolean hasSensitiveWord(String text) {
        if (text == null || text.isEmpty()) return false;
        for (String w : wordFilterMapper.getAllWords()) { if (text.contains(w)) return true; }
        return false;
    }

    public Map<String, Object> commentStats() {
        Map<String, Object> s = new HashMap<>();
        List<Comment> all = commentMapper.selectList(new LambdaQueryWrapper<Comment>().eq(Comment::getIsShow, 1));
        int total = all.size(); double avg = all.stream().mapToInt(Comment::getScore).average().orElse(0);
        long imgCount = all.stream().filter(c -> c.getImgList() != null && !c.getImgList().isEmpty()).count();
        long pending = all.stream().filter(c -> c.getReplyContent() == null || c.getReplyContent().isEmpty()).count();
        s.put("total", total); s.put("avgRate", Math.round(avg * 100.0 / 5.0));
        s.put("imageCount", (int) imgCount); s.put("pendingReply", (int) pending); return s;
    }

    public void batchHide(int[] ids) {
        for (int id : ids) hideComment(id);
    }

    private List<Map<String, Object>> enrichComments(List<Comment> list) {
        List<Map<String, Object>> r = new ArrayList<>();
        for (Comment c : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId()); m.put("productId", c.getProductId()); m.put("userId", c.getUserId());
            m.put("score", c.getScore()); m.put("content", c.getContent());
            m.put("imgList", c.getImgList() != null && !c.getImgList().isEmpty() ? c.getImgList().split(",") : new String[0]);
            m.put("isAnonymous", c.getIsAnonymous()); m.put("createTime", c.getCreateTime());
            m.put("isShow", c.getIsShow()); m.put("replyContent", c.getReplyContent()); m.put("likeCount", c.getLikeCount());
            // 查询商品名称
            String goodsName = "";
            if (c.getProductId() != null) {
                Product prod = productMapper.selectById(c.getProductId());
                goodsName = prod != null ? prod.getName() : "";
            }
            m.put("goodsName", goodsName);
            // 查询买家用户名
            String username = "";
            if (c.getUserId() != null) {
                User user = userMapper.selectById(c.getUserId());
                username = user != null ? user.getUsername() : "";
            }
            m.put("username", username); m.put("nickname", username);
            r.add(m);
        }
        return r;
    }
}
