package com.testpilot.platform.auth;

import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.user.User;
import com.testpilot.platform.user.UserRepository;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, Long> sessions = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        User user = userRepository.findByUsernameAndStatus(request.username(), "ENABLED")
                .orElseThrow(() -> new BusinessException(40101, "用户名或密码错误"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(40101, "用户名或密码错误");
        }
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        String token = UUID.randomUUID().toString();
        sessions.put(token, user.getId());
        return new AuthDtos.LoginResponse(token, AuthDtos.UserResponse.from(user));
    }

    public User currentUser(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(40100, "未登录");
        }
        Long userId = sessions.get(authorization.substring("Bearer ".length()).trim());
        if (userId == null) {
            throw new BusinessException(40100, "登录已失效");
        }
        return userRepository.findByIdAndStatus(userId, "ENABLED")
                .orElseThrow(() -> new BusinessException(40100, "登录已失效"));
    }

    public void logout(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            sessions.remove(authorization.substring("Bearer ".length()).trim());
        }
    }
}
