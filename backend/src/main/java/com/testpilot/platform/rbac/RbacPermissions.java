package com.testpilot.platform.rbac;

public final class RbacPermissions {

    private RbacPermissions() {
    }

    public static final String SYSTEM_USER_MANAGE = "system:user:manage";
    public static final String SYSTEM_ROLE_MANAGE = "system:role:manage";
    public static final String SYSTEM_AI_MODEL_MANAGE = "system:ai-model:manage";
    public static final String SYSTEM_JENKINS_MANAGE = "system:jenkins:manage";
    public static final String SYSTEM_AUDIT_READ = "system:audit:read";

    public static final String PROJECT_READ = "project:read";
    public static final String PROJECT_UPDATE = "project:update";
    public static final String PROJECT_MEMBER_MANAGE = "project:member:manage";

    public static final String REQUIREMENT_READ = "requirement:read";
    public static final String REQUIREMENT_WRITE = "requirement:write";
    public static final String TESTCASE_READ = "testcase:read";
    public static final String TESTCASE_WRITE = "testcase:write";
    public static final String TESTCASE_REVIEW = "testcase:review";
    public static final String API_EXECUTE = "api:execute";
    public static final String AUTOMATION_EXECUTE = "automation:execute";
    public static final String REPORT_READ = "report:read";
    public static final String AI_GENERATE = "ai:generate";
    public static final String AI_ANALYZE = "ai:analyze";
}
