package com.jiege.jieaiagent.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jiege.jieaiagent.mapper.UserMapper;
import com.jiege.jieaiagent.model.User;
import com.jiege.jieaiagent.util.JwtUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class AuthController {

    @Resource
    private UserMapper userMapper;

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String nickname = body.getOrDefault("nickname", username);

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Map.of("code", 400, "message", "用户名和密码不能为空");
        }

        Long count = userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (count > 0) {
            return Map.of("code", 400, "message", "用户名已存在");
        }

        String salt = generateSalt();
        String hash = sha256(salt + password);
        String passwordHash = salt + ":" + hash;

        User user = User.builder()
            .username(username)
            .passwordHash(passwordHash)
            .nickname(nickname)
            .build();
        userMapper.insert(user);

        String token = JwtUtil.generate(user.getId(), username);

        return Map.of("code", 0, "message", "注册成功", "data", Map.of(
            "token", token,
            "user", Map.of("id", user.getId(), "username", username, "nickname", nickname)
        ));
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Map.of("code", 400, "message", "用户名和密码不能为空");
        }

        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, username));

        if (user == null) {
            return Map.of("code", 400, "message", "用户名或密码错误");
        }

        String[] parts = user.getPasswordHash().split(":", 2);
        if (parts.length != 2) {
            return Map.of("code", 500, "message", "密码格式错误");
        }
        String salt = parts[0];
        String storedHash = parts[1];
        String computedHash = sha256(salt + password);

        if (!storedHash.equals(computedHash)) {
            return Map.of("code", 400, "message", "用户名或密码错误");
        }

        String token = JwtUtil.generate(user.getId(), user.getUsername());

        return Map.of("code", 0, "message", "登录成功", "data", Map.of(
            "token", token,
            "user", Map.of("id", user.getId(), "username", user.getUsername(), "nickname", user.getNickname())
        ));
    }

    @GetMapping("/info")
    public Map<String, Object> info(HttpServletRequest request) {
        User user = (User) request.getAttribute("currentUser");
        if (user == null) {
            return Map.of("code", 401, "message", "未登录");
        }
        return Map.of("code", 0, "data", Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "nickname", user.getNickname()
        ));
    }

    @PostMapping("/logout")
    public Map<String, Object> logout() {
        return Map.of("code", 0, "message", "已退出登录");
    }

    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
