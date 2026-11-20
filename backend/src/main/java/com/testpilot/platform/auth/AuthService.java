package com.testpilot.platform.auth;

import com.testpilot.platform.audit.OperationAuditService;
import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.user.User;
import com.testpilot.platform.user.UserRepository;
import com.testpilot.platform.auth.AuthSessionStore.AuthSession;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OperationAuditService operationAuditService;
    private final AuthSessionStore sessionStore;
    private final LoginAttemptTracker loginAttemptTracker;
    private final long sessionTtlMinutes;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            OperationAuditService operationAuditService,
            AuthSessionStore sessionStore,
            LoginAttemptTracker loginAttemptTracker,
            @Value("${testpilot.security.session-ttl-minutes:480}") long sessionTtlMinutes
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.operationAuditService = operationAuditService;
        this.sessionStore = sessionStore;
        this.loginAttemptTracker = loginAttemptTracker;
        this.sessionTtlMinutes = sessionTtlMinutes;
    }

    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        return login(request, null);
    }

    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request, String clientIp) {
        String username = request.username() == null ? "" : request.username().trim();
        if (loginAttemptTracker.isLocked(username, clientIp)) {
            operationAuditService.record(null, null, "LOGIN", "USER", null,
                    "登录被限流：用户名 " + username, false);
            throw new BusinessException(42901, "登录尝试过于频繁，请稍后再试");
        }
        User user = userRepository.findByUsernameAndStatus(username, "ENABLED").orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptTracker.recordFailure(username, clientIp);
            operationAuditService.record(null, null, "LOGIN", "USER", null,
                    "登录失败：用户名 " + username, false);
            throw new BusinessException(40101, "用户名或密码错误");
        }
        loginAttemptTracker.clear(username, clientIp);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        String token = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        sessionStore.save(token, new AuthSession(
                user.getId(),
                user.getAuthVersion(),
                now,
                now.plusMinutes(sessionTtlMinutes)
        ));
        operationAuditService.record(null, user, "LOGIN", "USER", user.getId(), "用户登录成功", true);
        return new AuthDtos.LoginResponse(token, AuthDtos.UserResponse.from(user));
    }

    public User currentUser(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(40100, "未登录");
        }
        String token = authorization.substring("Bearer ".length()).trim();
        AuthSession session = sessionStore.get(token);
        if (session == null || session.expiresAt().isBefore(LocalDateTime.now())) {
            sessionStore.delete(token);
            throw new BusinessException(40100, "登录已失效");
        }
        User user = userRepository.findByIdAndStatus(session.userId(), "ENABLED")
                .orElseThrow(() -> new BusinessException(40100, "登录已失效"));
        if (!session.authVersion().equals(user.getAuthVersion())) {
            sessionStore.delete(token);
            throw new BusinessException(40100, "登录已失效");
        }
        return user;
    }

    public void logout(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            sessionStore.delete(authorization.substring("Bearer ".length()).trim());
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public void invalidateUserSessions(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }
        user.bumpAuthVersion();
        userRepository.save(user);
    }
}
