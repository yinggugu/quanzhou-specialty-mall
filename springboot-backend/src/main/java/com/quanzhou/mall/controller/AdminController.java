package com.quanzhou.mall.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quanzhou.mall.bean.*;
import com.quanzhou.mall.config.ApiResponse;
import com.quanzhou.mall.mapper.*;
import com.quanzhou.mall.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired private ProductMapper productMapper;
    @Autowired private OrderMapper orderMapper;
    @Autowired private OrderItemMapper orderItemMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private CommentMapper commentMapper;
    @Autowired private AfterSaleMapper afterSaleMapper;
    @Autowired private ChatService chatService;
    @Autowired private CommentService commentService;
    @Autowired private AfterSaleService afterSaleService;

    // === 仪表盘 ===
    @GetMapping("/dashboard")
    public ApiResponse<?> dashboard() {
        Map<String,Object> s = new HashMap<>();
        s.put("totalProducts", productMapper.selectCount(null));
        s.put("totalOrders", orderMapper.selectCount(null));
        s.put("totalUsers", userMapper.selectCount(null));
        s.put("todayOrders", orderMapper.selectCount(new LambdaQueryWrapper<Order>().ge(Order::getCreateTime, new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date()))));
        Map<String,Object> cs = commentService.commentStats(); Map<String,Object> as = afterSaleService.stats();
        s.put("totalComments", cs.get("total")); s.put("avgCommentRate", cs.get("avgRate")); s.put("imageCommentCount", cs.get("imageCount"));
        s.put("commentPendingReply", cs.get("pendingReply")); s.put("afterSalePending", as.get("pending")); s.put("afterSaleMonthTotal", as.get("monthTotal")); s.put("afterSaleFinished", as.get("finished"));
        return ApiResponse.ok(s);
    }

    // === 商品管理 ===
    @GetMapping("/products")
    public ApiResponse<?> productList(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="10") int pageSize, @RequestParam(required=false) String goodsName) {
        LambdaQueryWrapper<Product> q = new LambdaQueryWrapper<>();
        if (goodsName != null && !goodsName.trim().isEmpty()) q.like(Product::getName, goodsName.trim());
        q.orderByDesc(Product::getId);
        Page<Product> p = productMapper.selectPage(new Page<>(page, pageSize), q);
        Map<String,Object> r = new HashMap<>(); r.put("list", p.getRecords()); r.put("totalCount", p.getTotal()); r.put("totalPages", p.getPages()); r.put("currentPage", page); return ApiResponse.ok(r);
    }

    @GetMapping("/products/{id}") public ApiResponse<?> productDetail(@PathVariable int id) { Product p = productMapper.selectById(id); return p != null ? ApiResponse.ok(p) : ApiResponse.error("商品不存在"); }
    @PostMapping("/products") public ApiResponse<?> productAdd(@RequestBody Product p) { p.setId(null); productMapper.insert(p); return ApiResponse.ok("添加成功"); }
    @PutMapping("/products/{id}") public ApiResponse<?> productUpdate(@PathVariable int id, @RequestBody Product p) { p.setId(id); productMapper.updateById(p); return ApiResponse.ok("更新成功"); }
    @DeleteMapping("/products/{id}") public ApiResponse<?> productDelete(@PathVariable int id) { productMapper.deleteById(id); return ApiResponse.ok("删除成功"); }
    @PatchMapping("/products/{id}/status") public ApiResponse<?> productStatus(@PathVariable int id, @RequestBody Map<String,Object> b) { Product p = productMapper.selectById(id); if (p != null) { p.setStatus(toInt(b.get("status"))); productMapper.updateById(p); } return ApiResponse.ok("已更新"); }

    // === 订单管理 ===
    @GetMapping("/orders")
    public ApiResponse<?> orderList(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="10") int pageSize) {
        Page<Order> p = orderMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<Order>().orderByDesc(Order::getCreateTime));
        for (Order o : p.getRecords()) o.setOrderItems(orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, o.getId())));
        Map<String,Object> r = new HashMap<>(); r.put("list", p.getRecords()); r.put("totalCount", p.getTotal()); r.put("totalPages", p.getPages()); return ApiResponse.ok(r);
    }
    @GetMapping("/orders/{id}")
    public ApiResponse<?> orderDetail(@PathVariable int id) { Order o = orderMapper.selectById(id); if (o != null) o.setOrderItems(orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id))); return o != null ? ApiResponse.ok(o) : ApiResponse.error("订单不存在"); }
    @PatchMapping("/orders/{id}/status") public ApiResponse<?> orderStatus(@PathVariable Integer id, @RequestBody Map<String,Object> b) { Order o = orderMapper.selectById(id); if (o != null) { o.setStatus(toInt(b.get("status"))); orderMapper.updateById(o); } return ApiResponse.ok("已更新"); }
    @DeleteMapping("/orders/{id}") public ApiResponse<?> orderDelete(@PathVariable int id) { orderMapper.deleteById(id); return ApiResponse.ok("删除成功"); }

    // === 用户管理 ===
    @GetMapping("/users")
    public ApiResponse<?> userList(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="10") int pageSize) {
        Page<User> p = userMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<User>().orderByDesc(User::getId));
        Map<String,Object> r = new HashMap<>(); r.put("list", p.getRecords()); r.put("totalCount", p.getTotal()); r.put("totalPages", p.getPages()); return ApiResponse.ok(r);
    }
    @GetMapping("/users/{id}") public ApiResponse<?> userDetail(@PathVariable int id) { User u = userMapper.selectById(id); return u != null ? ApiResponse.ok(u) : ApiResponse.error("用户不存在"); }
    @PutMapping("/users/{id}") public ApiResponse<?> userUpdate(@PathVariable int id, @RequestBody User u) { u.setId(id); userMapper.updateById(u); return ApiResponse.ok("更新成功"); }
    @DeleteMapping("/users/{id}") public ApiResponse<?> userDelete(@PathVariable int id) { userMapper.deleteById(id); return ApiResponse.ok("删除成功"); }

    // === 聊天管理 ===
    @PutMapping("/chat/markUnread") public ApiResponse<?> markUnread(@RequestBody Map<String,Object> b) { chatService.markUnread(toInt(b.get("sessionId"))); return ApiResponse.ok("已标记"); }
    @PutMapping("/chat/delSessionMsg") public ApiResponse<?> delSessionMsg(@RequestBody Map<String,Object> b) { chatService.softDeleteSessionMessages(toInt(b.get("sessionId"))); return ApiResponse.ok("已删除"); }

    // === 评价管理 ===
    @GetMapping("/comment/list") public ApiResponse<?> commentList(@RequestParam(defaultValue="") String goodsName, @RequestParam(defaultValue="") String scoreFilter, @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="10") int pageSize) { return ApiResponse.ok(commentService.adminCommentList(goodsName, scoreFilter, page, pageSize)); }
    @PutMapping("/comment/hide") public ApiResponse<?> hideComment(@RequestBody Map<String,Object> b) { commentService.hideComment(toInt(b.get("id"))); return ApiResponse.ok("已删除"); }
    @PutMapping("/comment/reply") public ApiResponse<?> replyComment(@RequestBody Map<String,Object> b) { commentService.replyComment(toInt(b.get("id")), (String)b.get("reply")); return ApiResponse.ok("已回复"); }
    @GetMapping("/comment/words") public ApiResponse<?> getWords() { return ApiResponse.ok(commentService.getWords()); }
    @PostMapping("/comment/words") public ApiResponse<?> addWord(@RequestBody Map<String,Object> b) { commentService.addWord((String)b.get("word")); return ApiResponse.ok("已添加"); }
    @PutMapping("/comment/words/{id}") public ApiResponse<?> updateWord(@PathVariable int id, @RequestBody Map<String,Object> b) { commentService.updateWord(id, (String)b.get("word")); return ApiResponse.ok("已修改"); }
    @DeleteMapping("/comment/words/{id}") public ApiResponse<?> deleteWord(@PathVariable int id) { commentService.deleteWord(id); return ApiResponse.ok("已删除"); }
    @PostMapping("/comment/batchDelete") public ApiResponse<?> batchDeleteComment(@RequestBody Map<String,Object> b) { List<Integer> ids = (List<Integer>)b.get("ids"); commentService.batchHide(ids.stream().mapToInt(i->i).toArray()); return ApiResponse.ok("批量删除成功"); }
    @GetMapping("/comment/stats") public ApiResponse<?> commentStats() { return ApiResponse.ok(commentService.commentStats()); }

    // === 售后管理 ===
    @GetMapping("/afterSale/list") public ApiResponse<?> afterSaleList(@RequestParam(defaultValue="") String orderNo, @RequestParam(defaultValue="") String username, @RequestParam(defaultValue="") String expressNo, @RequestParam(required=false) Integer status, @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="10") int pageSize) { return ApiResponse.ok(afterSaleService.adminList(orderNo, username, expressNo, status, page, pageSize)); }
    @GetMapping("/afterSale/detail") public ApiResponse<?> afterSaleDetail(@RequestParam int id) { return ApiResponse.ok(afterSaleService.detail(id)); }
    @PostMapping("/afterSale/examine") public ApiResponse<?> afterSaleExamine(@RequestBody Map<String,Object> b, @RequestAttribute(name="userId",required=false) Integer adminId) { boolean approve = (boolean)b.getOrDefault("approve", true); String reject = (String)b.get("rejectContent"); if (!approve && (reject == null || reject.trim().isEmpty())) return ApiResponse.error("驳回必须填写理由"); afterSaleService.examine(toInt(b.get("id")), approve, reject, adminId != null ? adminId : 0); return ApiResponse.ok(approve ? "审核通过" : "已驳回"); }
    @PostMapping("/afterSale/receiveGoods") public ApiResponse<?> afterSaleReceive(@RequestBody Map<String,Object> b, @RequestAttribute(name="userId",required=false) Integer adminId) { afterSaleService.receiveGoods(toInt(b.get("id")), (String)b.get("imgList"), adminId != null ? adminId : 0); return ApiResponse.ok("已确认收货"); }
    @PostMapping("/afterSale/refund") public ApiResponse<?> afterSaleRefund(@RequestBody Map<String,Object> b, @RequestAttribute(name="userId",required=false) Integer adminId) { Map<String,Object> r = afterSaleService.refund(toInt(b.get("id")), Double.parseDouble(b.get("amount").toString()), adminId != null ? adminId : 0, (String)b.get("refundImgs"), (String)b.get("refundDesc")); int code = (int)r.getOrDefault("code", 200); return code == 200 ? ApiResponse.ok(r.get("msg")) : ApiResponse.error(code, (String)r.get("msg")); }
    @PostMapping("/afterSale/batchDelete") public ApiResponse<?> afterSaleBatchDelete(@RequestBody Map<String,Object> b) { afterSaleService.batchDelete((List<Integer>)b.get("ids")); return ApiResponse.ok("批量删除成功"); }
    @DeleteMapping("/afterSale/delete/{id}") public ApiResponse<?> afterSaleDelete(@PathVariable int id) { afterSaleService.delete(id); return ApiResponse.ok("已删除"); }
    @GetMapping("/afterSale/stats") public ApiResponse<?> afterSaleStats() { return ApiResponse.ok(afterSaleService.stats()); }

    private int toInt(Object o) { if (o == null) return 0; if (o instanceof Number) return ((Number)o).intValue(); return Integer.parseInt(o.toString()); }
}
