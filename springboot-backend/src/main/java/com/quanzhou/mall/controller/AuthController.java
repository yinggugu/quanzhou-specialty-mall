package com.quanzhou.mall.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quanzhou.mall.bean.Admin;
import com.quanzhou.mall.bean.User;
import com.quanzhou.mall.config.ApiResponse;
import com.quanzhou.mall.mapper.AdminMapper;
import com.quanzhou.mall.mapper.UserMapper;
import com.quanzhou.mall.util.JwtUtil;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private UserMapper userMapper;
    @Autowired private AdminMapper adminMapper;
    @Autowired private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ApiResponse<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String email = body.get("email");
        String phone = body.get("phone");
        String address = body.get("address");

        if (username == null || !username.matches("^[a-zA-Z0-9_]{3,20}$"))
            return ApiResponse.error("用户名只能包含字母、数字和下划线，长度3-20个字符");
        if (password == null || password.length() < 6 || password.length() > 20)
            return ApiResponse.error("密码长度需要6-20个字符");
        if (!password.matches(".*[a-zA-Z].*") || !password.matches(".*\\d.*"))
            return ApiResponse.error("密码需要包含字母和数字");
        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            return ApiResponse.error("请输入有效的邮箱地址");
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$"))
            return ApiResponse.error("请输入有效的手机号码");

        // 查重
        if (userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username).or().eq(User::getEmail, email).or().eq(User::getPhone, phone)) != null)
            return ApiResponse.error("用户名、邮箱或手机号已被注册");

        User user = new User();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setEmail(email);
        user.setPhone(phone);
        user.setAddress(address != null ? address : "");
        user.setStatus(1);
        userMapper.insert(user);
        return ApiResponse.ok("注册成功");
    }

    @PostMapping("/userLogin")
    public ApiResponse<?> userLogin(@RequestBody Map<String, String> body) {
        return doLogin(body, "user");
    }

    @PostMapping("/adminLogin")
    public ApiResponse<?> adminLogin(@RequestBody Map<String, String> body) {
        return doLogin(body, "admin");
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        if (username == null || username.trim().isEmpty()) return ApiResponse.error("请输入用户名");
        // 先查 user 表
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username.trim()).eq(User::getStatus, 1));
        if (user != null) return doLogin(body, "user");
        // 再查 admin 表
        return doLogin(body, "admin");
    }

    private ApiResponse<?> doLogin(Map<String, String> body, String type) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || username.trim().isEmpty()) return ApiResponse.error("请输入用户名");
        if (password == null || password.isEmpty()) return ApiResponse.error("请输入密码");

        if ("user".equals(type)) {
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username.trim()).eq(User::getStatus, 1));
            if (user == null) return ApiResponse.error("用户名或密码错误");
            if (!checkPwd(password, user.getPassword())) return ApiResponse.error("用户名或密码错误");
            if (autoUpgradePwd(password, user.getPassword())) {
                user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
                userMapper.updateById(user);
            }
            return buildResp(user.getId(), username, type, user.getEmail(), user.getPhone(), user.getAddress(), null, user.getStatus());
        } else {
            Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, username.trim()).eq(Admin::getStatus, 1));
            if (admin == null) return ApiResponse.error("用户名或密码错误");
            if (!checkPwd(password, admin.getPassword())) return ApiResponse.error("用户名或密码错误");
            if (autoUpgradePwd(password, admin.getPassword())) {
                admin.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
                adminMapper.updateById(admin);
            }
            return buildResp(admin.getId(), username, type, admin.getEmail(), admin.getPhone(), null, admin.getName(), admin.getStatus());
        }
    }

    private boolean checkPwd(String raw, String stored) {
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$"))
            return BCrypt.checkpw(raw, stored);
        return raw.equals(stored);
    }

    private boolean autoUpgradePwd(String raw, String stored) {
        return !stored.startsWith("$2a$") && !stored.startsWith("$2b$") && !stored.startsWith("$2y$") && raw.equals(stored);
    }

    private ApiResponse<?> buildResp(Integer id, String username, String type, String email, String phone, String address, String name, Integer status) {
        String token = jwtUtil.generateToken(id, username, type);
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", id); userInfo.put("username", username); userInfo.put("type", type);
        if (name != null) userInfo.put("name", name);
        userInfo.put("email", email != null ? email : ""); userInfo.put("phone", phone != null ? phone : "");
        if (address != null) userInfo.put("address", address);
        userInfo.put("status", status);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token); result.put("user", userInfo);
        return ApiResponse.ok("登录成功", result);
    }

    @PostMapping("/logout")
    public ApiResponse<?> logout() { return ApiResponse.ok("已退出登录"); }
}
