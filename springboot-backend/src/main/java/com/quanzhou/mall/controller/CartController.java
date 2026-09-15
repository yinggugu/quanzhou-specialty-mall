package com.quanzhou.mall.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quanzhou.mall.bean.CartItem;
import com.quanzhou.mall.bean.Product;
import com.quanzhou.mall.config.ApiResponse;
import com.quanzhou.mall.mapper.CartItemMapper;
import com.quanzhou.mall.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired private CartItemMapper cartItemMapper;
    @Autowired private ProductMapper productMapper;

    @GetMapping
    public ApiResponse<?> list(@RequestAttribute(name="userId",required=false) Integer userId) {
        if (userId == null) return ApiResponse.error(401, "请先登录");
        List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId));
        for (CartItem ci : items) { Product p = productMapper.selectById(ci.getProductId()); if (p != null) { ci.setProductName(p.getName()); ci.setProductPrice(p.getPrice()); ci.setProductImage(p.getImage()); ci.setTotalPrice(p.getPrice() * ci.getQuantity()); } }
        int totalCount = items.stream().mapToInt(CartItem::getQuantity).sum();
        double totalPrice = items.stream().mapToDouble(i -> i.getProductPrice() * i.getQuantity()).sum();
        Map<String,Object> r = new HashMap<>();
        r.put("items", items); r.put("totalCount", totalCount); r.put("totalPrice", Math.round(totalPrice*100)/100.0);
        return ApiResponse.ok(r);
    }

    @PostMapping("/items")
    public ApiResponse<?> add(@RequestAttribute(name="userId",required=false) Integer userId, @RequestBody CartItem body) {
        if (userId == null) return ApiResponse.error(401, "请先登录");
        Product p = productMapper.selectById(body.getProductId());
        if (p == null || p.getStatus() != 1) return ApiResponse.error("商品不存在或已下架");
        if (p.getStock() == null || p.getStock() <= 0) return ApiResponse.error("商品库存不足");
        // 已有则加数量
        CartItem exist = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId).eq(CartItem::getProductId, body.getProductId()));
        if (exist != null) {
            exist.setQuantity(exist.getQuantity() + (body.getQuantity() > 0 ? body.getQuantity() : 1));
            if (exist.getQuantity() > p.getStock()) exist.setQuantity(p.getStock());
            cartItemMapper.updateById(exist);
        } else {
            CartItem ci = new CartItem();
            ci.setUserId(userId); ci.setProductId(p.getId()); ci.setProductName(p.getName());
            ci.setProductPrice(p.getPrice()); ci.setProductImage(p.getImage());
            int quantity = body.getQuantity() > 0 ? body.getQuantity() : 1;
            ci.setQuantity(Math.min(quantity, p.getStock()));
            cartItemMapper.insert(ci);
        }
        return ApiResponse.ok("已加入购物车");
    }

    @PutMapping("/items/{id}")
    public ApiResponse<?> update(@PathVariable Integer id, @RequestBody CartItem body,
            @RequestAttribute(name="userId",required=false) Integer userId) {
        if (userId == null) return ApiResponse.error(401, "请先登录");
        CartItem ci = cartItemMapper.selectById(id);
        if (ci == null) return ApiResponse.error("购物车项不存在");
        if (!ci.getUserId().equals(userId)) return ApiResponse.error(403, "无权操作该购物车项");
        Product product = productMapper.selectById(ci.getProductId());
        if (product == null || product.getStatus() != 1) return ApiResponse.error("商品不存在或已下架");
        if (body.getQuantity() == null || body.getQuantity() <= 0) return ApiResponse.error("商品数量必须大于0");
        if (body.getQuantity() > product.getStock()) return ApiResponse.error("商品库存不足");
        ci.setQuantity(body.getQuantity());
        cartItemMapper.updateById(ci);
        return ApiResponse.ok("已更新");
    }

    @DeleteMapping("/items/{id}")
    public ApiResponse<?> delete(@PathVariable Integer id,
            @RequestAttribute(name="userId",required=false) Integer userId) {
        if (userId == null) return ApiResponse.error(401, "请先登录");
        CartItem ci = cartItemMapper.selectById(id);
        if (ci == null) return ApiResponse.error("购物车项不存在");
        if (!ci.getUserId().equals(userId)) return ApiResponse.error(403, "无权操作该购物车项");
        cartItemMapper.deleteById(id);
        return ApiResponse.ok("已删除");
    }

    @DeleteMapping
    public ApiResponse<?> clear(@RequestAttribute(name="userId",required=false) Integer userId) {
        if (userId == null) return ApiResponse.error(401, "请先登录");
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId));
        return ApiResponse.ok("购物车已清空");
    }

    @GetMapping("/checkout")
    public ApiResponse<?> checkout(@RequestAttribute(name="userId",required=false) Integer userId) {
        if (userId == null) return ApiResponse.error(401, "请先登录");
        List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId));
        for (CartItem ci : items) { Product p = productMapper.selectById(ci.getProductId()); if (p != null) { ci.setProductName(p.getName()); ci.setProductPrice(p.getPrice()); ci.setProductImage(p.getImage()); ci.setTotalPrice(p.getPrice() * ci.getQuantity()); } }
        int totalCount = items.stream().mapToInt(CartItem::getQuantity).sum();
        double totalPrice = items.stream().mapToDouble(i -> i.getProductPrice() * i.getQuantity()).sum();
        Map<String,Object> r = new HashMap<>();
        r.put("items", items); r.put("totalCount", totalCount); r.put("totalPrice", Math.round(totalPrice*100)/100.0);
        return ApiResponse.ok(r);
    }
}
