package com.testpilot.platform.auth;

import com.testpilot.platform.audit.OperationAuditService;
import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.user.User;
import com.testpilot.platform.user.UserRepository;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OperationAuditService operationAuditService;
    private final long sessionTtlMinutes;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            OperationAuditService operationAuditService,
            @Value("${testpilot.security.session-ttl-minutes:480}") long sessionTtlMinutes
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.operationAuditService = operationAuditService;
        this.sessionTtlMinutes = sessionTtlMinutes;
    }

    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        User user = userRepository.findByUsernameAndStatus(request.username(), "ENABLED").orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            operationAuditService.record(null, null, "LOGIN", "USER", null,
                    "登录失败：用户名 " + request.username(), false);
            throw new BusinessException(40101, "用户名或密码错误");
        }
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        String token = UUID.randomUUID().toString();
        sessions.put(token, new Session(user.getId(), LocalDateTime.now().plusMinutes(sessionTtlMinutes)));
        operationAuditService.record(null, user, "LOGIN", "USER", user.getId(), "用户登录成功", true);
        return new AuthDtos.LoginResponse(token, AuthDtos.UserResponse.from(user));
    }

    public User currentUser(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(40100, "未登录");
        }
        String token = authorization.substring("Bearer ".length()).trim();
        Session session = sessions.get(token);
        if (session == null || session.expiresAt().isBefore(LocalDateTime.now())) {
            sessions.remove(token);
            throw new BusinessException(40100, "登录已失效");
        }
        return userRepository.findByIdAndStatus(session.userId(), "ENABLED")
                .orElseThrow(() -> new BusinessException(40100, "登录已失效"));
    }

    public void logout(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            sessions.remove(authorization.substring("Bearer ".length()).trim());
        }
    }

    private record Session(Long userId, LocalDateTime expiresAt) {
    }
}
