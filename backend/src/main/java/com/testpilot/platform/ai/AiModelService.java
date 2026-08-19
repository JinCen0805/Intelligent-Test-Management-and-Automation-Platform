package com.testpilot.platform.ai;

import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.common.SecretCipher;
import com.testpilot.platform.user.User;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiModelService {

    private final AiModelRepository aiModelRepository;
    private final PromptTemplateRepository promptTemplateRepository;
    private final SecretCipher secretCipher;

    public AiModelService(
            AiModelRepository aiModelRepository,
            PromptTemplateRepository promptTemplateRepository,
            SecretCipher secretCipher
    ) {
        this.aiModelRepository = aiModelRepository;
        this.promptTemplateRepository = promptTemplateRepository;
        this.secretCipher = secretCipher;
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
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new BusinessException(40303, "只有管理员可以管理 AI 配置");
        }
    }
}
