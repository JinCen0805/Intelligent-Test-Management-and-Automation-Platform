package com.testpilot.platform.rbac;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
public class RbacBootstrap {

    private final RbacService rbacService;

    public RbacBootstrap(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncLegacyAssignmentsAfterStartup() {
        rbacService.syncLegacyAssignments();
    }
}
