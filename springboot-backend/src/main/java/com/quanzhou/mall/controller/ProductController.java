package com.quanzhou.mall.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quanzhou.mall.bean.Product;
import com.quanzhou.mall.config.ApiResponse;
import com.quanzhou.mall.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired private ProductMapper productMapper;

    @GetMapping
    public ApiResponse<?> list(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="12") int pageSize,
            @RequestParam(required=false) Integer categoryId, @RequestParam(required=false) String goodsName) {
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<>();
        qw.eq(Product::getStatus, 1);
        if (categoryId != null && categoryId > 0) qw.eq(Product::getCategoryId, categoryId);
        if (goodsName != null && !goodsName.trim().isEmpty()) qw.like(Product::getName, goodsName.trim());
        qw.orderByAsc(Product::getSort).orderByDesc(Product::getId);
        Page<Product> p = productMapper.selectPage(new Page<>(page, pageSize), qw);
        Map<String,Object> r = new HashMap<>();
        r.put("list", p.getRecords()); r.put("totalCount", p.getTotal());
        r.put("totalPages", p.getPages()); r.put("pages", p.getPages()); r.put("currentPage", page);
        return ApiResponse.ok(r);
    }

    @GetMapping("/recommended")
    public ApiResponse<?> recommended(@RequestParam(defaultValue="3") int limit) {
        List<Product> list = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1).orderByDesc(Product::getSort).last("LIMIT " + limit));
        return ApiResponse.ok(list);
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable Integer id) {
        Product p = productMapper.selectById(id);
        return p != null ? ApiResponse.ok(p) : ApiResponse.error(404, "商品不存在");
    }

    @GetMapping("/categories")
    public ApiResponse<?> categories() {
        // 直接查询所有商品的唯一分类ID并返回
        List<Product> list = productMapper.selectList(new LambdaQueryWrapper<Product>().select(Product::getCategoryId).groupBy(Product::getCategoryId));
        List<Map<String,Object>> cats = new ArrayList<>();
        String[] names = {"","茶叶","糕点","海鲜","工艺品"};
        for (Product p : list) {
            int cid = p.getCategoryId();
            if (cid > 0 && cid < names.length) {
                Map<String,Object> m = new HashMap<>(); m.put("id", cid); m.put("name", names[cid]); cats.add(m);
            }
        }
        return ApiResponse.ok(cats);
    }
}
