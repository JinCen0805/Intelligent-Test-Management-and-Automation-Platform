package com.testpilot.platform.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.project.ProjectAccessService;
import com.testpilot.platform.rbac.RbacPermissions;
import com.testpilot.platform.user.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiCaseChatSessionService {

    private static final String DEFAULT_SUITE_TYPE = AiGenerationService.SUITE_FUNCTIONAL;

    private static final TypeReference<List<AiDtos.CaseChatMessageResponse>> MESSAGE_LIST_TYPE =
            new TypeReference<>() {
            };

    private final AiCaseChatSessionRepository repository;
    private final ProjectAccessService projectAccessService;
    private final ObjectMapper objectMapper;

    public AiCaseChatSessionService(
            AiCaseChatSessionRepository repository,
            ProjectAccessService projectAccessService,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.projectAccessService = projectAccessService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AiDtos.CaseChatSessionResponse appendTurn(
            Long projectId,
            User currentUser,
            String suiteType,
            Long sessionId,
            String userMessage,
            String assistantReply
    ) {
        projectAccessService.requirePermission(projectId, currentUser, RbacPermissions.AI_GENERATE);
        String normalizedSuiteType = normalizeSuiteType(suiteType);
        Long normalizedSessionId = sessionId == null ? System.currentTimeMillis() : sessionId;
        AiCaseChatSession session = repository.findByProjectIdAndUserIdAndSuiteTypeAndSessionKey(
                projectId,
                currentUser.getId(),
                normalizedSuiteType,
                normalizedSessionId
        ).orElseGet(() -> new AiCaseChatSession(projectId, currentUser.getId(), normalizedSuiteType, normalizedSessionId, "新对话", "[]"));
        List<AiDtos.CaseChatMessageResponse> messages = new ArrayList<>(fromJson(session.getMessagesJson()));
        long now = System.currentTimeMillis();
        messages.add(new AiDtos.CaseChatMessageResponse(now, "user", userMessage));
        messages.add(new AiDtos.CaseChatMessageResponse(now + 1, "assistant", assistantReply));
        session.setTitle(buildTitle(messages));
        session.setMessagesJson(toJson(messages));
        return toResponse(repository.save(session));
    }

    @Transactional(readOnly = true)
    public List<AiDtos.CaseChatSessionResponse> list(Long projectId, String suiteType, User currentUser) {
        projectAccessService.requirePermission(projectId, currentUser, RbacPermissions.AI_GENERATE);
        String normalizedSuiteType = normalizeSuiteType(suiteType);
        return repository.findAllByProjectIdAndUserIdAndSuiteTypeOrderByUpdatedAtDesc(projectId, currentUser.getId(), normalizedSuiteType).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<AiDtos.CaseChatSessionResponse> latest(Long projectId, String suiteType, User currentUser) {
        return list(projectId, suiteType, currentUser).stream().findFirst();
    }

    @Transactional
    public void delete(Long projectId, String suiteType, Long sessionId, User currentUser) {
        projectAccessService.requirePermission(projectId, currentUser, RbacPermissions.AI_GENERATE);
        repository.deleteByProjectIdAndUserIdAndSuiteTypeAndSessionKey(projectId, currentUser.getId(), normalizeSuiteType(suiteType), sessionId);
    }

    @Transactional
    public void deleteAll(Long projectId, String suiteType, User currentUser) {
        projectAccessService.requirePermission(projectId, currentUser, RbacPermissions.AI_GENERATE);
        repository.deleteAllByProjectIdAndUserIdAndSuiteType(projectId, currentUser.getId(), normalizeSuiteType(suiteType));
    }

    private AiDtos.CaseChatSessionResponse toResponse(AiCaseChatSession session) {
        List<AiDtos.CaseChatMessageResponse> messages = fromJson(session.getMessagesJson());
        return new AiDtos.CaseChatSessionResponse(
                session.getSessionKey(),
                session.getTitle(),
                messages,
                session.getSuiteType(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }

    private String normalizeSuiteType(String suiteType) {
        String value = suiteType == null ? "" : suiteType.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case AiGenerationService.SUITE_API, AiGenerationService.SUITE_FUNCTIONAL -> value;
            default -> DEFAULT_SUITE_TYPE;
        };
    }

    private List<AiDtos.CaseChatMessageResponse> fromJson(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, MESSAGE_LIST_TYPE);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(50001, "历史对话数据读取失败");
        }
    }

    private String toJson(List<AiDtos.CaseChatMessageResponse> messages) {
        try {
            return objectMapper.writeValueAsString(messages == null ? List.of() : messages);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(50002, "历史对话数据保存失败");
        }
    }

    private String buildTitle(List<AiDtos.CaseChatMessageResponse> messages) {
        if (messages == null || messages.isEmpty()) {
            return "新对话";
        }
        for (AiDtos.CaseChatMessageResponse message : messages) {
            if ("user".equals(message.role()) && message.content() != null && !message.content().isBlank()) {
                return truncate(message.content().trim(), 24);
            }
        }
        return truncate(messages.get(messages.size() - 1).content(), 24);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "新对话";
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "新对话";
        }
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
