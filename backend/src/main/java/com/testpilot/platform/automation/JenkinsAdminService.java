package com.testpilot.platform.automation;

import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.common.AddressSafety;
import com.testpilot.platform.common.SecretCipher;
import com.testpilot.platform.project.ProjectAccessService;
import com.testpilot.platform.rbac.RbacPermissions;
import com.testpilot.platform.user.User;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class JenkinsAdminService {

    private static final List<String> PIPELINE_TYPES = List.of(
            "MULTIBRANCH_PIPELINE", "PIPELINE_JOB", "FREESTYLE_JOB"
    );
    private static final List<String> MANAGED_BY = List.of("PLATFORM", "EXTERNAL");

    private final JenkinsInstanceRepository instanceRepository;
    private final JenkinsPipelineBindingRepository bindingRepository;
    private final ProjectAccessService projectAccessService;
    private final AutomationProjectRepository automationProjectRepository;
    private final JenkinsCredentialGrantRepository credentialGrantRepository;
    private final SecretCipher secretCipher;
    private final JenkinsClient jenkinsClient;
    private final JenkinsProtocolService jenkinsProtocolService;
    private final AddressSafety addressSafety;
    private final TransactionTemplate transactionTemplate;

    public JenkinsAdminService(
            JenkinsInstanceRepository instanceRepository,
            JenkinsPipelineBindingRepository bindingRepository,
            ProjectAccessService projectAccessService,
            AutomationProjectRepository automationProjectRepository,
            JenkinsCredentialGrantRepository credentialGrantRepository,
            SecretCipher secretCipher,
            JenkinsClient jenkinsClient,
            JenkinsProtocolService jenkinsProtocolService,
            AddressSafety addressSafety,
            TransactionTemplate transactionTemplate
    ) {
        this.instanceRepository = instanceRepository;
        this.bindingRepository = bindingRepository;
        this.projectAccessService = projectAccessService;
        this.automationProjectRepository = automationProjectRepository;
        this.credentialGrantRepository = credentialGrantRepository;
        this.secretCipher = secretCipher;
        this.jenkinsClient = jenkinsClient;
        this.jenkinsProtocolService = jenkinsProtocolService;
        this.addressSafety = addressSafety;
        this.transactionTemplate = transactionTemplate;
    }

    public List<JenkinsAdminDtos.InstanceResponse> listInstances(User user) {
        requireAdmin(user);
        // 列表仅返回连接配置元数据，不返回 Token 或最近一次错误详情。
        return instanceRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(instance -> JenkinsAdminDtos.InstanceResponse.from(instance, false))
                .toList();
    }

    @Transactional
    public JenkinsAdminDtos.InstanceResponse createInstance(
            JenkinsAdminDtos.InstanceRequest request, User user
    ) {
        requireAdmin(user);
        validateBaseUrl(request.baseUrl());
        if (request.apiToken() == null || request.apiToken().isBlank()) {
            throw new BusinessException(40051, "Jenkins API Token 不能为空");
        }
        JenkinsInstance instance = new JenkinsInstance(
                request.name().trim(),
                normalizeBaseUrl(request.baseUrl()),
                request.username().trim(),
                secretCipher.encrypt(request.apiToken().trim()),
                user.getId(),
                valueOr(request.connectTimeoutMs(), 5000),
                valueOr(request.readTimeoutMs(), 15000),
                valueOr(request.pollIntervalMs(), 10000),
                valueOr(request.maxWaitMinutes(), 30)
        );
        return JenkinsAdminDtos.InstanceResponse.from(instanceRepository.save(instance));
    }

    @Transactional
    public JenkinsAdminDtos.InstanceResponse updateInstance(
            Long instanceId, JenkinsAdminDtos.InstanceRequest request, User user
    ) {
        requireAdmin(user);
        JenkinsInstance instance = requireInstance(instanceId);
        validateBaseUrl(request.baseUrl());
        instance.update(
                request.name().trim(),
                normalizeBaseUrl(request.baseUrl()),
                request.username().trim(),
                valueOr(request.connectTimeoutMs(), instance.getConnectTimeoutMs()),
                valueOr(request.readTimeoutMs(), instance.getReadTimeoutMs()),
                valueOr(request.pollIntervalMs(), instance.getPollIntervalMs()),
                valueOr(request.maxWaitMinutes(), instance.getMaxWaitMinutes())
        );
        if (request.apiToken() != null && !request.apiToken().isBlank()) {
            instance.replaceToken(secretCipher.encrypt(request.apiToken().trim()));
        }
        return JenkinsAdminDtos.InstanceResponse.from(instanceRepository.save(instance));
    }

    @Transactional
    public JenkinsAdminDtos.InstanceResponse changeStatus(
            Long instanceId, JenkinsAdminDtos.StatusRequest request, User user
    ) {
        requireAdmin(user);
        JenkinsInstance instance = requireInstance(instanceId);
        String status = request.status() == null ? "" : request.status().trim().toUpperCase(Locale.ROOT);
        if ("ENABLED".equals(status)) {
            instance.enable();
        } else if ("DISABLED".equals(status)) {
            instance.disable();
        } else {
            throw new BusinessException(40058, "实例状态只能是 ENABLED 或 DISABLED");
        }
        return JenkinsAdminDtos.InstanceResponse.from(instanceRepository.save(instance));
    }

    @Transactional
    public void deleteInstance(Long instanceId, User user) {
        requireAdmin(user);
        JenkinsInstance instance = requireInstance(instanceId);
        instance.disable();
        instanceRepository.save(instance);
    }

    /** 连接测试是外部 HTTP 调用，不占用数据库事务；仅结果回写是单条写操作。 */
    public JenkinsAdminDtos.InstanceResponse testConnection(Long instanceId, User user) {
        requireAdmin(user);
        JenkinsInstance instance = requireInstance(instanceId);
        try {
            jenkinsClient.testConnection(toTarget(instance));
            instance.markConnection("CONNECTED", null);
        } catch (BusinessException exception) {
            instance.markConnection("FAILED", exception.getMessage());
        }
        return JenkinsAdminDtos.InstanceResponse.from(instanceRepository.save(instance));
    }

    public List<JenkinsAdminDtos.BindingResponse> listBindings(
            Long projectId, Long automationProjectId, User user
    ) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.AUTOMATION_EXECUTE);
        return bindingRepository.findAllByProjectIdAndAutomationProjectIdOrderByCreatedAtAsc(
                        projectId, automationProjectId
                ).stream()
                .map(JenkinsAdminDtos.BindingResponse::from)
                .toList();
    }

    public List<JenkinsAdminDtos.JobResponse> listJobs(
            Long projectId, Long automationProjectId, Long jenkinsInstanceId, User user
    ) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.AUTOMATION_EXECUTE);
        requireAutomationProject(projectId, automationProjectId);
        JenkinsInstance instance = requireEnabledInstance(jenkinsInstanceId);
        // 一次取齐该项目已绑定的 job，避免每个 Jenkins Job 都查一次库。
        java.util.Set<String> boundJobNames = bindingRepository
                .findAllByProjectIdAndAutomationProjectIdOrderByCreatedAtAsc(projectId, automationProjectId)
                .stream()
                .filter(binding -> instance.getId().equals(binding.getJenkinsInstanceId()))
                .map(JenkinsPipelineBinding::getJobName)
                .collect(java.util.stream.Collectors.toSet());
        return jenkinsClient.listJobs(toTarget(instance)).stream()
                .map(job -> JenkinsAdminDtos.JobResponse.from(job, boundJobNames.contains(job.path())))
                .toList();
    }

    public JenkinsAdminDtos.BindingResponse importJob(
            Long projectId,
            Long automationProjectId,
            JenkinsAdminDtos.JobImportRequest request,
            User user
    ) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.AUTOMATION_EXECUTE);
        AutomationProject automationProject = requireAutomationProject(projectId, automationProjectId);
        JenkinsInstance instance = requireEnabledInstance(request.jenkinsInstanceId());
        validateCredentialScope(projectId, request.credentialScopeRef());
        String jobPath = request.jobName().trim();
        JenkinsClient.JenkinsJob job = jenkinsClient.listJobs(toTarget(instance)).stream()
                .filter(candidate -> candidate.path().equals(jobPath))
                .findFirst()
                .orElseThrow(() -> new BusinessException(40453, "Jenkins Job 不存在或不在授权范围内"));

        // 外部 Jenkins 调用结束后，仅把绑定落库放进短事务。
        return transactionTemplate.execute(status -> {
            var existing = bindingRepository
                    .findByProjectIdAndAutomationProjectIdAndJenkinsInstanceIdAndJobName(
                            projectId, automationProjectId, instance.getId(), job.path()
                    );
            if (existing.isPresent()) {
                return JenkinsAdminDtos.BindingResponse.from(existing.get());
            }
            JenkinsPipelineBinding binding = new JenkinsPipelineBinding(
                    projectId,
                    automationProjectId,
                    instance.getId(),
                    job.pipelineType(),
                    parentPath(job.path()),
                    job.path(),
                    job.url(),
                    blankToNull(request.credentialScopeRef()),
                    "EXTERNAL",
                    blankToNull(job.parametersJson())
            );
            if (automationProject.getJenkinsJob() == null || automationProject.getJenkinsJob().isBlank()) {
                automationProject.setJenkinsJob(job.path());
                automationProjectRepository.save(automationProject);
            }
            return JenkinsAdminDtos.BindingResponse.from(bindingRepository.save(binding));
        });
    }

    public JenkinsAdminDtos.BindingResponse createJobAndBind(
            Long projectId, Long automationProjectId, JenkinsAdminDtos.CreateJobRequest request, User user
    ) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.AUTOMATION_EXECUTE);
        AutomationProject automationProject = requireAutomationProject(projectId, automationProjectId);
        JenkinsInstance instance = requireEnabledInstance(request.jenkinsInstanceId());
        validateCredentialScope(projectId, request.credentialScopeRef());
        String pipelineType = normalizeType(request.pipelineType());
        if (!List.of("PIPELINE_JOB", "FREESTYLE_JOB").contains(pipelineType)) {
            throw new BusinessException(40060, "平台仅支持创建 Pipeline Job 或 Freestyle Job");
        }
        String configXml;
        if ("PIPELINE_JOB".equals(pipelineType)) {
            String script = "CUSTOM".equals(normalizeType(request.scriptSource()))
                    && request.customScript() != null && !request.customScript().isBlank()
                    ? request.customScript() : jenkinsProtocolService.pipelineTemplate(automationProject);
            configXml = jenkinsProtocolService.pipelineConfigXml(automationProject, script);
        } else {
            configXml = jenkinsProtocolService.freestyleConfigXml(automationProject);
        }
        String jobName = request.jobName().trim();
        jenkinsClient.createJob(toTarget(instance), jobName, configXml);

        // Jenkins Job 创建成功后，绑定与项目回写放进短事务。
        return transactionTemplate.execute(status -> {
            var existing = bindingRepository
                    .findByProjectIdAndAutomationProjectIdAndJenkinsInstanceIdAndJobName(
                            projectId, automationProjectId, instance.getId(), jobName
                    );
            if (existing.isPresent()) {
                return JenkinsAdminDtos.BindingResponse.from(existing.get());
            }
            JenkinsPipelineBinding binding = new JenkinsPipelineBinding(
                    projectId,
                    automationProjectId,
                    instance.getId(),
                    pipelineType,
                    parentPath(jobName),
                    jobName,
                    buildJobUrl(instance.getBaseUrl(), jobName),
                    blankToNull(request.credentialScopeRef()),
                    "PLATFORM",
                    null
            );
            if (automationProject.getJenkinsJob() == null || automationProject.getJenkinsJob().isBlank()) {
                automationProject.setJenkinsJob(jobName);
                automationProjectRepository.save(automationProject);
            }
            return JenkinsAdminDtos.BindingResponse.from(bindingRepository.save(binding));
        });
    }

    @Transactional
    public JenkinsAdminDtos.BindingResponse createBinding(
            Long projectId, Long automationProjectId, JenkinsAdminDtos.BindingRequest request, User user
    ) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.AUTOMATION_EXECUTE);
        requireAutomationProject(projectId, automationProjectId);
        JenkinsInstance instance = requireEnabledInstance(request.jenkinsInstanceId());
        validateBinding(request);
        validateCredentialScope(projectId, request.credentialScopeRef());
        JenkinsPipelineBinding binding = new JenkinsPipelineBinding(
                projectId,
                automationProjectId,
                instance.getId(),
                normalizeType(request.pipelineType()),
                blankToNull(request.folderPath()),
                request.jobName().trim(),
                blankToNull(request.jobUrl()),
                blankToNull(request.credentialScopeRef()),
                normalizeManagedBy(request.managedBy()),
                blankToNull(request.parametersJson())
        );
        return JenkinsAdminDtos.BindingResponse.from(bindingRepository.save(binding));
    }

    @Transactional
    public JenkinsAdminDtos.BindingResponse updateBinding(
            Long projectId, Long bindingId, JenkinsAdminDtos.BindingRequest request, User user
    ) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.AUTOMATION_EXECUTE);
        JenkinsPipelineBinding binding = bindingRepository.findByIdAndProjectId(bindingId, projectId)
                .orElseThrow(() -> new BusinessException(40452, "Pipeline Binding 不存在"));
        requireEnabledInstance(request.jenkinsInstanceId());
        validateBinding(request);
        validateCredentialScope(projectId, request.credentialScopeRef());
        binding.setJenkinsInstanceId(request.jenkinsInstanceId());
        binding.update(
                normalizeType(request.pipelineType()),
                blankToNull(request.folderPath()),
                request.jobName().trim(),
                blankToNull(request.jobUrl()),
                blankToNull(request.credentialScopeRef()),
                normalizeManagedBy(request.managedBy()),
                blankToNull(request.parametersJson())
        );
        return JenkinsAdminDtos.BindingResponse.from(bindingRepository.save(binding));
    }

    public JenkinsClient.JenkinsTarget toTarget(JenkinsInstance instance) {
        return new JenkinsClient.JenkinsTarget(
                instance.getBaseUrl(),
                instance.getUsername(),
                secretCipher.decrypt(instance.getApiTokenCiphertext()),
                instance.getConnectTimeoutMs(),
                instance.getReadTimeoutMs()
        );
    }

    public JenkinsInstance requireEnabledInstance(Long id) {
        if (id == null) {
            throw new BusinessException(40052, "必须选择 Jenkins 实例");
        }
        return instanceRepository.findByIdAndStatus(id, "ENABLED")
                .orElseThrow(() -> new BusinessException(40451, "Jenkins 实例不存在或已禁用"));
    }

    public JenkinsInstance requireInstance(Long id) {
        return instanceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(40451, "Jenkins 实例不存在"));
    }

    public List<JenkinsAdminDtos.CredentialGrantResponse> listCredentialGrants(
            Long projectId, User user
    ) {
        projectAccessService.requirePermission(projectId, user, RbacPermissions.AUTOMATION_EXECUTE);
        return credentialGrantRepository.findAllByProjectIdAndStatusOrderByCredentialRefAsc(
                        projectId, "ACTIVE"
                ).stream()
                .map(JenkinsAdminDtos.CredentialGrantResponse::from)
                .toList();
    }

    @Transactional
    public JenkinsAdminDtos.CredentialGrantResponse createCredentialGrant(
            Long projectId,
            JenkinsAdminDtos.CredentialGrantRequest request,
            User user
    ) {
        requireAdmin(user);
        projectAccessService.requirePermission(projectId, user, RbacPermissions.AUTOMATION_EXECUTE);
        String credentialRef = request.credentialRef().trim();
        if (credentialGrantRepository.existsByProjectIdAndCredentialRefAndStatus(
                projectId, credentialRef, "ACTIVE")) {
            throw new BusinessException(40921, "该项目已授权此凭证");
        }
        JenkinsCredentialGrant grant = new JenkinsCredentialGrant(
                projectId,
                credentialRef,
                blankToNull(request.credentialScopeRef()),
                user.getId()
        );
        return JenkinsAdminDtos.CredentialGrantResponse.from(credentialGrantRepository.save(grant));
    }

    @Transactional
    public void revokeCredentialGrant(Long projectId, Long grantId, User user) {
        requireAdmin(user);
        projectAccessService.requirePermission(projectId, user, RbacPermissions.AUTOMATION_EXECUTE);
        JenkinsCredentialGrant grant = credentialGrantRepository.findByIdAndProjectId(grantId, projectId)
                .orElseThrow(() -> new BusinessException(40455, "凭证授权不存在"));
        grant.revoke();
        credentialGrantRepository.save(grant);
    }

    public void validateCredentialRef(Long projectId, String credentialRef) {
        if (credentialRef != null && !credentialRef.isBlank()
                && !credentialGrantRepository.existsByProjectIdAndCredentialRefAndStatus(
                        projectId, credentialRef.trim(), "ACTIVE")) {
            throw new BusinessException(40311, "凭证未获得当前项目授权");
        }
    }

    public void validateCredentialScope(Long projectId, String credentialScopeRef) {
        if (credentialScopeRef != null && !credentialScopeRef.isBlank()
                && !credentialGrantRepository.existsByProjectIdAndCredentialScopeRefAndStatus(
                        projectId, credentialScopeRef.trim(), "ACTIVE")) {
            throw new BusinessException(40312, "凭证作用域未获得当前项目授权");
        }
    }

    private void requireAdmin(User user) {
        if (!projectAccessService.isAdmin(user)) {
            throw new AccessDeniedException("只有管理员可以管理 Jenkins 实例");
        }
    }

    private void validateBinding(JenkinsAdminDtos.BindingRequest request) {
        if (!PIPELINE_TYPES.contains(normalizeType(request.pipelineType()))) {
            throw new BusinessException(40053, "Pipeline 类型不支持");
        }
        if (request.jobName() == null || request.jobName().isBlank()) {
            throw new BusinessException(40054, "Job 名称不能为空");
        }
        if (request.parametersJson() != null && !request.parametersJson().isBlank()
                && !(request.parametersJson().trim().startsWith("{")
                || request.parametersJson().trim().startsWith("["))) {
            throw new BusinessException(40055, "Binding 参数必须是 JSON");
        }
    }

    private AutomationProject requireAutomationProject(Long projectId, Long automationProjectId) {
        return automationProjectRepository.findByIdAndProjectId(automationProjectId, projectId)
                .orElseThrow(() -> new BusinessException(40454, "自动化项目不存在"));
    }

    private String parentPath(String path) {
        int separator = path.lastIndexOf('/');
        return separator < 0 ? null : path.substring(0, separator);
    }

    private String buildJobUrl(String baseUrl, String jobName) {
        StringBuilder url = new StringBuilder(baseUrl == null ? "" : baseUrl.replaceAll("/+$", ""));
        for (String segment : jobName.split("/")) {
            if (!segment.isBlank()) {
                url.append("/job/").append(segment);
            }
        }
        return url.toString();
    }

    private String normalizeType(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeManagedBy(String value) {
        String normalized = value == null || value.isBlank() ? "EXTERNAL" : normalizeType(value);
        if (!MANAGED_BY.contains(normalized)) {
            throw new BusinessException(40056, "资源管理类型不支持");
        }
        return normalized;
    }

    private void validateBaseUrl(String value) {
        try {
            URI uri = URI.create(value.trim());
            if (!List.of("http", "https").contains(uri.getScheme())
                    || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null) {
                throw new IllegalArgumentException();
            }
            addressSafety.validate(value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(40057, "Jenkins 地址必须是合法的 HTTP 或 HTTPS 地址");
        }
    }

    private String normalizeBaseUrl(String value) {
        return value.trim().replaceAll("/+$", "");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private int valueOr(Integer value, int fallback) {
        return value == null ? fallback : value;
    }
}
