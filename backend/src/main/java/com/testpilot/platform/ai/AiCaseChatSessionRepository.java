package com.testpilot.platform.ai;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.common.persistence.MybatisRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AiCaseChatSessionRepository extends MybatisRepository<AiCaseChatSession, Long> {

    public AiCaseChatSessionRepository(AiCaseChatSessionMapper mapper) {
        super(mapper, AiCaseChatSession.class, AiCaseChatSession::getId);
    }

    public List<AiCaseChatSession> findAllByProjectIdAndUserIdAndSuiteTypeOrderByUpdatedAtDesc(Long projectId, Long userId, String suiteType) {
        return list(Wrappers.<AiCaseChatSession>lambdaQuery()
                .eq(AiCaseChatSession::getProjectId, projectId)
                .eq(AiCaseChatSession::getUserId, userId)
                .eq(AiCaseChatSession::getSuiteType, suiteType)
                .orderByDesc(AiCaseChatSession::getUpdatedAt));
    }

    public Optional<AiCaseChatSession> findByProjectIdAndUserIdAndSuiteTypeAndSessionKey(Long projectId, Long userId, String suiteType, Long sessionKey) {
        return findOne(Wrappers.<AiCaseChatSession>lambdaQuery()
                .eq(AiCaseChatSession::getProjectId, projectId)
                .eq(AiCaseChatSession::getUserId, userId)
                .eq(AiCaseChatSession::getSuiteType, suiteType)
                .eq(AiCaseChatSession::getSessionKey, sessionKey));
    }

    public void deleteByProjectIdAndUserIdAndSuiteTypeAndSessionKey(Long projectId, Long userId, String suiteType, Long sessionKey) {
        delete(Wrappers.<AiCaseChatSession>lambdaQuery()
                .eq(AiCaseChatSession::getProjectId, projectId)
                .eq(AiCaseChatSession::getUserId, userId)
                .eq(AiCaseChatSession::getSuiteType, suiteType)
                .eq(AiCaseChatSession::getSessionKey, sessionKey));
    }

    public void deleteAllByProjectIdAndUserIdAndSuiteType(Long projectId, Long userId, String suiteType) {
        delete(Wrappers.<AiCaseChatSession>lambdaQuery()
                .eq(AiCaseChatSession::getProjectId, projectId)
                .eq(AiCaseChatSession::getUserId, userId)
                .eq(AiCaseChatSession::getSuiteType, suiteType));
    }
}
