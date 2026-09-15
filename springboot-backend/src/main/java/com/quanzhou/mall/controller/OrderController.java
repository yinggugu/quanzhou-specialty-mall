package com.quanzhou.mall.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quanzhou.mall.bean.*;
import com.quanzhou.mall.config.ApiResponse;
import com.quanzhou.mall.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired private OrderMapper orderMapper;
    @Autowired private OrderItemMapper orderItemMapper;
    @Autowired private ProductMapper productMapper;
    @Autowired private CartItemMapper cartItemMapper;
    @Autowired private CommentMapper commentMapper;
    @Autowired private AfterSaleMapper afterSaleMapper;

    @GetMapping
    public ApiResponse<?> list(@RequestAttribute(name="userId",required=false) Integer userId,
            @RequestAttribute(name="type",required=false) String type,
            @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="10") int pageSize) {
        if (userId == null) return ApiResponse.error(401, "请先登录");
        LambdaQueryWrapper<Order> qw = new LambdaQueryWrapper<>();
        if (!"admin".equals(type)) qw.eq(Order::getUserId, userId);
        qw.orderByDesc(Order::getCreateTime);
        Page<Order> p = orderMapper.selectPage(new Page<>(page, pageSize), qw);
        for (Order o : p.getRecords()) {
            List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, o.getId()));
            for (OrderItem oi : items) { Product prod = productMapper.selectById(oi.getProductId()); if (prod != null) oi.setProductImage(prod.getImage()); }
            o.setOrderItems(items);
            // 查询该订单是否有进行中的售后
            AfterSale as = afterSaleMapper.selectOne(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getOrderId, o.getId())
                .notIn(AfterSale::getStatus, 4, 5)  // 排除已完结、已驳回
                .orderByDesc(AfterSale::getCreateTime).last("LIMIT 1"));
            if (as != null) {
                o.setAfterSaleId(as.getId());
                o.setAfterSaleStatus(as.getStatus());
            }
        }
        Map<String,Object> r = new HashMap<>();
        r.put("list", p.getRecords()); r.put("totalCount", p.getTotal());
        r.put("totalPages", p.getPages()); r.put("currentPage", page);
        return ApiResponse.ok(r);
    }

    // 获取订单中当前用户尚未评价的商品列表
    @GetMapping("/{id}/unreviewed-items")
    public ApiResponse<?> unreviewedItems(@PathVariable Integer id,
            @RequestAttribute(name="userId",required=false) Integer userId) {
        if (userId == null) return ApiResponse.error(401, "请先登录");
        Order o = orderMapper.selectById(id);
        if (o == null) return ApiResponse.error(404, "订单不存在");
        if (!o.getUserId().equals(userId)) return ApiResponse.error(403, "无权操作");
        List<OrderItem> items = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
        List<Map<String,Object>> result = new ArrayList<>();
        for (OrderItem oi : items) {
            // 检查该 orderItem 是否已被当前用户评价
            long count = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getUserId, userId)
                .eq(Comment::getOrderItemId, oi.getId()));
            if (count == 0) {
                Product p = productMapper.selectById(oi.getProductId());
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("id", oi.getId());
                m.put("productId", oi.getProductId());
                m.put("productName", oi.getProductName());
                m.put("productPrice", oi.getProductPrice());
                m.put("productImage", p != null ? p.getImage() : "");
                m.put("quantity", oi.getQuantity());
                m.put("totalPrice", oi.getTotalPrice());
                result.add(m);
            }
        }
        Map<String,Object> r = new HashMap<>();
        r.put("orderId", id);
        r.put("orderNo", o.getOrderNo());
        r.put("items", result);
        r.put("allReviewed", items.size() > 0 && result.isEmpty());
        return ApiResponse.ok(r);
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable Integer id,
            @RequestAttribute(name="userId",required=false) Integer userId) {
        if (userId == null) return ApiResponse.error(401, "请先登录");
        Order o = orderMapper.selectById(id);
        if (o == null) return ApiResponse.error(404, "订单不存在");
        if (!o.getUserId().equals(userId)) return ApiResponse.error(403, "无权查看该订单");
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
        for (OrderItem oi : items) { Product prod = productMapper.selectById(oi.getProductId()); if (prod != null) oi.setProductImage(prod.getImage()); }
        o.setOrderItems(items);
        // 查询售后状态
        AfterSale as = afterSaleMapper.selectOne(new LambdaQueryWrapper<AfterSale>()
            .eq(AfterSale::getOrderId, id)
            .notIn(AfterSale::getStatus, 4, 5)
            .orderByDesc(AfterSale::getCreateTime).last("LIMIT 1"));
        if (as != null) {
            o.setAfterSaleId(as.getId());
            o.setAfterSaleStatus(as.getStatus());
        }
        return ApiResponse.ok(o);
    }

    @PostMapping
    public ApiResponse<?> create(@RequestAttribute(name="userId",required=false) Integer userId,
            @RequestAttribute(name="username",required=false) String username,
            @RequestBody Map<String,Object> body) {
        if (userId == null) return ApiResponse.error(401, "请先登录");
        // 从数据库取购物车商品（防止前端篡改价格）
        List<CartItem> cartItems = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId));
        if (cartItems.isEmpty()) return ApiResponse.error(400, "购物车为空");
        double total = 0;
        for (CartItem ci : cartItems) {
            Product p = productMapper.selectById(ci.getProductId());
            if (p == null || p.getStatus() != 1) continue;
            total += p.getPrice() * ci.getQuantity();
        }
        String orderNo = "QZ" + System.currentTimeMillis() + (int)(Math.random()*900+100);
        Order o = new Order();
        o.setOrderNo(orderNo); o.setUserId(userId); o.setUsername(username != null ? username : "");
        o.setPhone((String)body.getOrDefault("phone",""));
        o.setAddress((String)body.getOrDefault("address",""));
        o.setTotalPrice(total); o.setStatus(0);
        orderMapper.insert(o);
        for (CartItem ci : cartItems) {
            Product p = productMapper.selectById(ci.getProductId());
            if (p == null || p.getStatus() != 1) continue;
            OrderItem oi = new OrderItem();
            oi.setOrderId(o.getId());
            oi.setProductId(p.getId());
            oi.setProductName(p.getName());
            oi.setProductPrice(p.getPrice());
            oi.setQuantity(ci.getQuantity());
            oi.setTotalPrice(p.getPrice() * ci.getQuantity());
            orderItemMapper.insert(oi);
        }
        // 下单后清空购物车
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId));
        Map<String,Object> r = new HashMap<>(); r.put("orderId", o.getId()); r.put("orderNo", orderNo);
        return ApiResponse.ok("下单成功", r);
    }

    @PostMapping("/{id}/pay")
    public ApiResponse<?> pay(@PathVariable Integer id,
            @RequestAttribute(name="userId",required=false) Integer userId) {
        if (userId == null) return ApiResponse.error(401, "请先登录");
        Order o = orderMapper.selectById(id);
        if (o == null) return ApiResponse.error(404, "订单不存在");
        if (!o.getUserId().equals(userId)) return ApiResponse.error(403, "无权操作该订单");
        if (o.getStatus() != 0) return ApiResponse.error("订单状态异常");
        o.setStatus(1); orderMapper.updateById(o); return ApiResponse.ok("支付成功");
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<?> cancel(@PathVariable Integer id,
            @RequestAttribute(name="userId",required=false) Integer userId) {
        if (userId == null) return ApiResponse.error(401, "请先登录");
        Order o = orderMapper.selectById(id);
        if (o == null) return ApiResponse.error(404, "订单不存在");
        if (!o.getUserId().equals(userId)) return ApiResponse.error(403, "无权操作该订单");
        if (o.getStatus() != 0) return ApiResponse.error("订单状态异常");
        o.setStatus(4); orderMapper.updateById(o); return ApiResponse.ok("订单已取消");
    }

    @PostMapping("/{id}/confirm-receipt")
    public ApiResponse<?> confirmReceipt(@PathVariable Integer id,
            @RequestAttribute(name="userId",required=false) Integer userId) {
        if (userId == null) return ApiResponse.error(401, "请先登录");
        Order o = orderMapper.selectById(id);
        if (o == null) return ApiResponse.error(404, "订单不存在");
        if (!o.getUserId().equals(userId)) return ApiResponse.error(403, "无权操作该订单");
        if (o.getStatus() != 2) return ApiResponse.error("订单状态异常");
        o.setStatus(3); orderMapper.updateById(o); return ApiResponse.ok("已确认收货");
    }
}
