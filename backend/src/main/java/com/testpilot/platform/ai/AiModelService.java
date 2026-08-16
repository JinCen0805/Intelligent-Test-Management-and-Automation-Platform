package com.testpilot.platform.ai;

import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.user.User;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiModelService {

    private final AiModelRepository aiModelRepository;
    private final PromptTemplateRepository promptTemplateRepository;

    public AiModelService(
            AiModelRepository aiModelRepository,
            PromptTemplateRepository promptTemplateRepository
    ) {
        this.aiModelRepository = aiModelRepository;
        this.promptTemplateRepository = promptTemplateRepository;
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
                request.apiKey(),
                request.enabled() == null || request.enabled()
        );
        return AiDtos.ModelResponse.from(aiModelRepository.save(model));
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
