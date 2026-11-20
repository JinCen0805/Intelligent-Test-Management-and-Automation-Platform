package com.testpilot.platform.config;

import com.testpilot.platform.rbac.RbacService;
import com.testpilot.platform.user.User;
import com.testpilot.platform.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 首次启动引导：数据库没有任何用户时，根据环境变量创建初始管理员。
 *
 * <p>开发/测试 profile 下允许留空（演示数据会创建账号）；其他环境必须显式配置
 * {@code ADMIN_INITIAL_USERNAME} 与 {@code ADMIN_INITIAL_PASSWORD}，否则启动失败并提示，
 * 避免部署出一个无法登录的空库。
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RbacService rbacService;
    private final Environment environment;
    private final String initialUsername;
    private final String initialPassword;

    public AdminBootstrap(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RbacService rbacService,
            Environment environment,
            @Value("${testpilot.security.admin-initial-username:}") String initialUsername,
            @Value("${testpilot.security.admin-initial-password:}") String initialPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.rbacService = rbacService;
        this.environment = environment;
        this.initialUsername = initialUsername;
        this.initialPassword = initialPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

        String username = initialUsername == null ? "" : initialUsername.trim();
        String password = initialPassword == null ? "" : initialPassword;
        if (username.isEmpty() || password.isEmpty()) {
            if (environment.acceptsProfiles(Profiles.of("local", "demo", "test"))) {
                log.info("未配置初始管理员账号，开发/测试环境跳过创建（演示数据会创建账号）");
                return;
            }
            throw new IllegalStateException(
                    "数据库没有任何用户：请配置 ADMIN_INITIAL_USERNAME 与 ADMIN_INITIAL_PASSWORD 后重启，或先导入管理员账号");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("ADMIN_INITIAL_PASSWORD 至少需要 " + MIN_PASSWORD_LENGTH + " 个字符");
        }

        try {
            User admin = userRepository.save(new User(
                    username,
                    passwordEncoder.encode(password),
                    "系统管理员",
                    null,
                    "ADMIN",
                    "ENABLED"
            ));
            log.warn("已创建初始管理员账号 '{}'，请登录后立即修改密码", admin.getUsername());
            // 空库场景同步 RBAC 目录与系统角色分配，保证管理员首次登录即有权限。
            rbacService.syncLegacyAssignments();
        } catch (DuplicateKeyException exception) {
            log.warn("初始管理员账号 '{}' 已存在（并发启动？），跳过创建", username);
        }
    }
}
