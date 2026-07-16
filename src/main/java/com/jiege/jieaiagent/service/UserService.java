package com.jiege.jieaiagent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jiege.jieaiagent.dto.LoginRequest;
import com.jiege.jieaiagent.dto.LoginResultVO;
import com.jiege.jieaiagent.dto.RegisterRequest;
import com.jiege.jieaiagent.dto.UserInfoVO;
import com.jiege.jieaiagent.exception.BusinessException;
import com.jiege.jieaiagent.mapper.UserMapper;
import com.jiege.jieaiagent.model.User;
import com.jiege.jieaiagent.util.JwtUtil;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private JwtUtil jwtUtil;

    public LoginResultVO register(RegisterRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();
        String nickname = request.getNickname() != null && !request.getNickname().isBlank()
                ? request.getNickname() : username;

        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .nickname(nickname)
                .role("USER")
                .build();
        userMapper.insert(user);

        String token = jwtUtil.generate(user.getId(), username, "USER");

        return LoginResultVO.builder()
                .token(token)
                .user(UserInfoVO.builder()
                        .id(user.getId())
                        .username(username)
                        .nickname(nickname)
                        .role("USER")
                        .build())
                .build();
    }

    public LoginResultVO login(LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));

        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }

        String token = jwtUtil.generate(user.getId(), user.getUsername(), user.getRole());

        return LoginResultVO.builder()
                .token(token)
                .user(UserInfoVO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .role(user.getRole())
                        .build())
                .build();
    }

    public UserInfoVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        return UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(user.getRole())
                .build();
    }

    public User getUserEntity(Long userId) {
        return userMapper.selectById(userId);
    }
}
