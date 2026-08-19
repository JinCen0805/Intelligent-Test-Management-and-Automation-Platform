package com.testpilot.platform.automation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class JenkinsAdminDtos {

    private JenkinsAdminDtos() {
    }

    public record InstanceRequest(
            @NotBlank(message = "Jenkins 实例名称不能为空")
            @Size(max = 128, message = "实例名称不能超过 128 个字符")
            String name,
            @NotBlank(message = "Jenkins 地址不能为空")
            @Size(max = 500, message = "Jenkins 地址不能超过 500 个字符")
            String baseUrl,
            @NotBlank(message = "服务账号不能为空")
            @Size(max = 128, message = "服务账号不能超过 128 个字符")
            String username,
            @Size(max = 1000, message = "Token 长度不能超过 1000 个字符")
            String apiToken,
            @Min(value = 100, message = "连接超时不能小于 100 毫秒")
            @Max(value = 120000, message = "连接超时不能超过 120 秒")
            Integer connectTimeoutMs,
            @Min(value = 100, message = "读取超时不能小于 100 毫秒")
            @Max(value = 300000, message = "读取超时不能超过 300 秒")
            Integer readTimeoutMs,
            @Min(value = 1000, message = "轮询间隔不能小于 1 秒")
            @Max(value = 300000, message = "轮询间隔不能超过 300 秒")
            Integer pollIntervalMs,
            @Min(value = 1, message = "最大等待时间不能小于 1 分钟")
            @Max(value = 1440, message = "最大等待时间不能超过 24 小时")
            Integer maxWaitMinutes
    ) {
    }

    public record InstanceResponse(
            Long id,
            String name,
            String baseUrl,
            String username,
            boolean tokenConfigured,
            String status,
            String connectionStatus,
            int connectTimeoutMs,
            int readTimeoutMs,
            int pollIntervalMs,
            int maxWaitMinutes,
            LocalDateTime lastCheckedAt,
            String lastError
    ) {
        static InstanceResponse from(JenkinsInstance instance) {
            return new InstanceResponse(
                    instance.getId(),
                    instance.getName(),
                    instance.getBaseUrl(),
                    instance.getUsername(),
                    instance.getApiTokenCiphertext() != null && !instance.getApiTokenCiphertext().isBlank(),
                    instance.getStatus(),
                    instance.getConnectionStatus(),
                    instance.getConnectTimeoutMs(),
                    instance.getReadTimeoutMs(),
                    instance.getPollIntervalMs(),
                    instance.getMaxWaitMinutes(),
                    instance.getLastCheckedAt(),
                    instance.getLastError()
            );
        }
    }

    public record StatusRequest(
            @NotBlank(message = "实例状态不能为空")
            String status
    ) {
    }

    public record BindingRequest(
            Long jenkinsInstanceId,
            @NotBlank(message = "Pipeline 类型不能为空")
            String pipelineType,
            String folderPath,
            @NotBlank(message = "Job 名称不能为空")
            String jobName,
            String jobUrl,
            String credentialScopeRef,
            String managedBy,
            String parametersJson
    ) {
    }

    public record BindingResponse(
            Long id,
            Long projectId,
            Long automationProjectId,
            Long jenkinsInstanceId,
            String pipelineType,
            String folderPath,
            String jobName,
            String jobUrl,
            String credentialScopeRef,
            String managedBy,
            String provisioningStatus,
            String parametersJson,
            LocalDateTime lastScanAt,
            String lastError
    ) {
        static BindingResponse from(JenkinsPipelineBinding binding) {
            return new BindingResponse(
                    binding.getId(),
                    binding.getProjectId(),
                    binding.getAutomationProjectId(),
                    binding.getJenkinsInstanceId(),
                    binding.getPipelineType(),
                    binding.getFolderPath(),
                    binding.getJobName(),
                    binding.getJobUrl(),
                    binding.getCredentialScopeRef(),
                    binding.getManagedBy(),
                    binding.getProvisioningStatus(),
                    binding.getParametersJson(),
                    binding.getLastScanAt(),
                    binding.getLastError()
            );
        }
    }

    public record JobResponse(
            String name,
            String path,
            String url,
            String pipelineType,
            String jenkinsClass,
            boolean buildable,
            String parametersJson,
            boolean imported
    ) {
        static JobResponse from(JenkinsClient.JenkinsJob job, boolean imported) {
            return new JobResponse(
                    job.name(),
                    job.path(),
                    job.url(),
                    job.pipelineType(),
                    job.jenkinsClass(),
                    job.buildable(),
                    job.parametersJson(),
                    imported
            );
        }
    }

    public record JobImportRequest(
            @jakarta.validation.constraints.NotNull(message = "必须选择 Jenkins 实例")
            Long jenkinsInstanceId,
            @jakarta.validation.constraints.NotBlank(message = "Job 名称不能为空")
            String jobName,
            String credentialScopeRef
    ) {
    }

    public record CreateJobRequest(
            @jakarta.validation.constraints.NotNull(message = "必须选择 Jenkins 实例")
            Long jenkinsInstanceId,
            @NotBlank(message = "Job 名称不能为空")
            String jobName,
            @NotBlank(message = "Pipeline 类型不能为空")
            String pipelineType,
            String scriptSource,
            String customScript,
            String credentialScopeRef
    ) {
    }

    public record CredentialGrantRequest(
            @jakarta.validation.constraints.NotBlank(message = "凭证引用不能为空")
            @jakarta.validation.constraints.Size(max = 128, message = "凭证引用长度不能超过 128 个字符")
            String credentialRef,
            @jakarta.validation.constraints.Size(max = 128, message = "凭证作用域长度不能超过 128 个字符")
            String credentialScopeRef
    ) {
    }

    public record CredentialGrantResponse(
            Long id,
            Long projectId,
            String credentialRef,
            String credentialScopeRef,
            String status
    ) {
        static CredentialGrantResponse from(JenkinsCredentialGrant grant) {
            return new CredentialGrantResponse(
                    grant.getId(),
                    grant.getProjectId(),
                    grant.getCredentialRef(),
                    grant.getCredentialScopeRef(),
                    grant.getStatus()
            );
        }
    }
}
