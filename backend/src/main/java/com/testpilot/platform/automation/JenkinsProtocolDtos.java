package com.testpilot.platform.automation;

import java.util.Map;

public final class JenkinsProtocolDtos {

    private JenkinsProtocolDtos() {
    }

    public record ContractResponse(
            String templateVersion,
            String automationType,
            String template,
            Map<String, String> parameters,
            String policy
    ) {
    }

    public record PrecheckResponse(
            String status,
            String executionMode,
            Long pipelineBindingId,
            Map<String, String> checks,
            String message
    ) {
    }
}
