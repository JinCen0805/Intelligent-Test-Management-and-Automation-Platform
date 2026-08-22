package com.testpilot.platform.ai;

import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.common.SecretCipher;
import com.testpilot.platform.rbac.RbacPermissions;
import com.testpilot.platform.rbac.RbacService;
import com.testpilot.platform.user.User;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AiModelService {

    private static final Logger log = LoggerFactory.getLogger(AiModelService.class);

    private final AiModelRepository aiModelRepository;
    private final PromptTemplateRepository promptTemplateRepository;
    private final SecretCipher secretCipher;
    private final AiProviderClient aiProviderClient;
    private final RbacService rbacService;

    public AiModelService(
            AiModelRepository aiModelRepository,
            PromptTemplateRepository promptTemplateRepository,
            SecretCipher secretCipher,
            AiProviderClient aiProviderClient,
            RbacService rbacService
    ) {
        this.aiModelRepository = aiModelRepository;
        this.promptTemplateRepository = promptTemplateRepository;
        this.secretCipher = secretCipher;
        this.aiProviderClient = aiProviderClient;
        this.rbacService = rbacService;
    }

    public List<AiDtos.ModelResponse> listModels(User user) {
        requireAdmin(user);
        return aiModelRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(AiDtos.ModelResponse::from)
                .toList();
    }

    public AiDtos.ModelResponse createModel(AiDtos.ModelRequest request, User user) {
        requireAdmin(user);
        AiModel model = new AiModel(
                request.name().trim(),
                request.provider().trim().toUpperCase(),
                request.endpoint() == null ? "" : request.endpoint().trim(),
                request.apiKey() == null || request.apiKey().isBlank()
                        ? null
                        : secretCipher.encrypt(request.apiKey().trim()),
                request.enabled() == null || request.enabled()
        );
        return AiDtos.ModelResponse.from(aiModelRepository.save(model));
    }

    public AiDtos.ModelResponse updateModel(Long id, AiDtos.ModelRequest request, User user) {
        requireAdmin(user);
        AiModel model = aiModelRepository.findById(id)
                .orElseThrow(() -> new BusinessException(40420, "AI 模型不存在"));
        String endpoint = request.endpoint() == null ? "" : request.endpoint().trim();
        String apiKey = request.apiKey() == null || request.apiKey().isBlank()
                ? model.getApiKey()
                : secretCipher.encrypt(request.apiKey().trim());
        model.update(
                request.name().trim(),
                request.provider().trim().toUpperCase(),
                endpoint,
                apiKey,
                request.enabled() == null ? model.isEnabled() : request.enabled()
        );
        return AiDtos.ModelResponse.from(aiModelRepository.save(model));
    }

    public AiDtos.ModelResponse setModelEnabled(Long id, boolean enabled, User user) {
        requireAdmin(user);
        AiModel model = aiModelRepository.findById(id)
                .orElseThrow(() -> new BusinessException(40420, "AI 模型不存在"));
        model.setEnabled(enabled);
        return AiDtos.ModelResponse.from(aiModelRepository.save(model));
    }

    public void testConnection(Long id, User user) {
        requireAdmin(user);
        AiModel model = aiModelRepository.findById(id)
                .orElseThrow(() -> new BusinessException(40420, "AI 模型不存在"));
        if ("MOCK".equalsIgnoreCase(model.getProvider())) {
            return;
        }
        // Probe only endpoint, credentials and model name; do not create project data.
        try {
            aiProviderClient.complete(model, "只回复 OK。", "连接测试：请只回复 OK。");
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("AI 模型连接测试异常: modelId={}, provider={}, endpoint={}",
                    model.getId(), model.getProvider(), model.getEndpoint(), exception);
            String detail = exception.getMessage();
            throw new BusinessException(
                    50200,
                    detail == null || detail.isBlank()
                            ? "AI 连接测试失败，请检查请求地址、API Key 和模型名称"
                            : "AI 连接测试失败：" + detail
            );
        }
    }

    public void deleteModel(Long id, User user) {
        requireAdmin(user);
        AiModel model = aiModelRepository.findById(id)
                .orElseThrow(() -> new BusinessException(40420, "AI 模型不存在"));
        aiModelRepository.delete(model);
    }

    public List<AiDtos.PromptResponse> listPrompts(User user) {
        requireAdmin(user);
        return promptTemplateRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(AiDtos.PromptResponse::from)
                .toList();
    }

    public AiDtos.PromptResponse createPrompt(AiDtos.PromptRequest request, User user) {
        requireAdmin(user);
        PromptTemplate prompt = new PromptTemplate(
                request.name().trim(),
                request.taskType().trim().toUpperCase(),
                request.content(),
                request.version().trim(),
                request.enabled() == null || request.enabled()
        );
        return AiDtos.PromptResponse.from(promptTemplateRepository.save(prompt));
    }

    public AiDtos.PromptResponse updatePrompt(Long id, AiDtos.PromptRequest request, User user) {
        requireAdmin(user);
        PromptTemplate prompt = promptTemplateRepository.findById(id)
                .orElseThrow(() -> new BusinessException(40421, "Prompt 模板不存在"));
        prompt.update(
                request.name().trim(),
                request.taskType().trim().toUpperCase(),
                request.content(),
                request.version().trim(),
                request.enabled() == null || request.enabled()
        );
        return AiDtos.PromptResponse.from(promptTemplateRepository.save(prompt));
    }

    public AiDtos.PromptResponse setPromptEnabled(Long id, boolean enabled, User user) {
        requireAdmin(user);
        PromptTemplate prompt = promptTemplateRepository.findById(id)
                .orElseThrow(() -> new BusinessException(40421, "Prompt 模板不存在"));
        prompt.setEnabled(enabled);
        return AiDtos.PromptResponse.from(promptTemplateRepository.save(prompt));
    }

    @Transactional
    public AiDtos.PromptResponse publishPrompt(Long id, String version, User user) {
        requireAdmin(user);
        PromptTemplate source = promptTemplateRepository.findById(id)
                .orElseThrow(() -> new BusinessException(40421, "Prompt 模板不存在"));
        String newVersion = version == null || version.isBlank() ? nextVersion(source) : version.trim();
        disableLineage(source.getName(), source.getTaskType());
        PromptTemplate published = promptTemplateRepository.save(new PromptTemplate(
                source.getName(),
                source.getTaskType(),
                source.getContent(),
                newVersion,
                true
        ));
        return AiDtos.PromptResponse.from(published);
    }

    @Transactional
    public AiDtos.PromptResponse activatePrompt(Long id, User user) {
        requireAdmin(user);
        PromptTemplate target = promptTemplateRepository.findById(id)
                .orElseThrow(() -> new BusinessException(40421, "Prompt 模板不存在"));
        disableLineage(target.getName(), target.getTaskType());
        target.setEnabled(true);
        return AiDtos.PromptResponse.from(promptTemplateRepository.save(target));
    }

    private void disableLineage(String name, String taskType) {
        for (PromptTemplate sibling : promptTemplateRepository.findByNameAndTaskType(name, taskType)) {
            if (sibling.isEnabled()) {
                sibling.setEnabled(false);
                promptTemplateRepository.save(sibling);
            }
        }
    }

    private String nextVersion(PromptTemplate source) {
        long count = promptTemplateRepository.findByNameAndTaskType(source.getName(), source.getTaskType()).size();
        return "V" + (count + 1) + ".0";
    }

    public AiModel findModel(Long modelId) {
        return modelId == null
                ? aiModelRepository.findFirstByEnabledTrueOrderByCreatedAtAsc()
                    .orElseThrow(() -> new BusinessException(40420, "没有可用的 AI 模型"))
                : aiModelRepository.findById(modelId)
                    .filter(AiModel::isEnabled)
                    .orElseThrow(() -> new BusinessException(40420, "AI 模型不存在或已停用"));
    }

    public PromptTemplate findPrompt(Long promptId, String taskType) {
        return promptId == null
                ? promptTemplateRepository.findFirstByTaskTypeAndEnabledTrueOrderByCreatedAtDesc(taskType)
                    .orElseThrow(() -> new BusinessException(40421, "没有可用的 Prompt 模板"))
                : promptTemplateRepository.findById(promptId)
                    .filter(PromptTemplate::isEnabled)
                    .orElseThrow(() -> new BusinessException(40421, "Prompt 模板不存在或已停用"));
    }

    private void requireAdmin(User user) {
        if (user == null || !rbacService.hasSystemPermission(user.getId(), RbacPermissions.SYSTEM_AI_MODEL_MANAGE)) {
            throw new AccessDeniedException("只有管理员可以管理 AI 配置");
        }
    }
}
