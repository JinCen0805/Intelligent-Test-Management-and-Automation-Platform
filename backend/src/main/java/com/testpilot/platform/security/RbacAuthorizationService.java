package com.testpilot.platform.security;

import com.testpilot.platform.rbac.RbacService;
import com.testpilot.platform.user.User;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("rbacAuthorizationService")
public class RbacAuthorizationService {

    private final RbacService rbacService;

    public RbacAuthorizationService(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    public boolean hasSystemPermission(Authentication authentication, String permissionCode) {
        User user = principal(authentication);
        return user != null && rbacService.hasSystemPermission(user.getId(), permissionCode);
    }

    public boolean hasProjectPermission(Authentication authentication, Long projectId, String permissionCode) {
        User user = principal(authentication);
        return user != null && projectId != null
                && rbacService.hasProjectPermission(user.getId(), projectId, permissionCode);
    }

    public boolean isAuthenticated(Authentication authentication) {
        return principal(authentication) != null;
    }

    private User principal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return null;
        }
        return user;
    }
}
