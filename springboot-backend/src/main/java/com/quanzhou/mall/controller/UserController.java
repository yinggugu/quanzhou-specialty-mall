package com.quanzhou.mall.controller;

import com.quanzhou.mall.bean.User;
import com.quanzhou.mall.config.ApiResponse;
import com.quanzhou.mall.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired private UserMapper userMapper;

    @GetMapping("/profile")
    public ApiResponse<?> profile(@RequestAttribute(name="userId",required=false) Integer userId) {
        if (userId == null) return ApiResponse.error(401, "请先登录");
        User user = userMapper.selectById(userId);
        return user != null ? ApiResponse.ok(user) : ApiResponse.error(404, "用户不存在");
    }

    @PutMapping("/profile")
    public ApiResponse<?> update(@RequestAttribute(name="userId",required=false) Integer userId, @RequestBody User userData) {
        if (userId == null) return ApiResponse.error(401, "请先登录");
        User user = userMapper.selectById(userId);
        if (user == null) return ApiResponse.error(404, "用户不存在");
        user.setEmail(userData.getEmail());
        user.setPhone(userData.getPhone());
        user.setAddress(userData.getAddress());
        userMapper.updateById(user);
        return ApiResponse.ok("更新成功");
    }
}
