package com.jiege.jieaiagent.config;

import com.jiege.jieaiagent.model.User;
import com.jiege.jieaiagent.service.UserService;
import com.jiege.jieaiagent.util.JwtUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeUnauthorized(response, "请先登录");
            return false;
        }

        String token = authHeader.substring(7);
        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            writeUnauthorized(response, "登录已过期，请重新登录");
            return false;
        }

        User user = userService.getUserEntity(userId);
        if (user == null) {
            writeUnauthorized(response, "用户不存在");
            return false;
        }

        if (path.startsWith("/api/admin/")) {
            if (!"ADMIN".equals(user.getRole())) {
                writeForbidden(response, "仅管理员可访问");
                return false;
            }
        }

        request.setAttribute("currentUser", user);
        return true;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\"}");
    }

    private void writeForbidden(HttpServletResponse response, String message) throws Exception {
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"message\":\"" + message + "\"}");
    }
}
