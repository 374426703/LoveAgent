package com.jiege.jieaiagent.controller;

import com.jiege.jieaiagent.dto.*;
import com.jiege.jieaiagent.model.User;
import com.jiege.jieaiagent.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class AuthController {

    @Resource
    private UserService userService;

    @PostMapping("/register")
    public ApiResponse register(@Valid @RequestBody RegisterRequest request) {
        LoginResultVO result = userService.register(request);
        return ApiResponse.success("注册成功", result);
    }

    @PostMapping("/login")
    public ApiResponse login(@Valid @RequestBody LoginRequest request) {
        LoginResultVO result = userService.login(request);
        return ApiResponse.success("登录成功", result);
    }

    @GetMapping("/info")
    public ApiResponse info(HttpServletRequest request) {
        User user = (User) request.getAttribute("currentUser");
        if (user == null) {
            return ApiResponse.error(401, "未登录");
        }
        UserInfoVO userInfo = userService.getCurrentUser(user.getId());
        return ApiResponse.success(userInfo);
    }

    @PostMapping("/logout")
    public ApiResponse logout() {
        return ApiResponse.success("已退出登录", Map.of());
    }
}
