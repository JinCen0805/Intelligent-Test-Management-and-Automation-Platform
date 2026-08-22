package com.testpilot.platform.ai;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.common.persistence.MybatisRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AiAutomationTaskDraftRepository extends MybatisRepository<AiAutomationTaskDraft, Long> {

    public AiAutomationTaskDraftRepository(AiAutomationTaskDraftMapper mapper) {
        super(mapper, AiAutomationTaskDraft.class, AiAutomationTaskDraft::getId);
    }

    public Optional<AiAutomationTaskDraft> findByIdAndProjectId(Long id, Long projectId) {
        return findOne(Wrappers.<AiAutomationTaskDraft>lambdaQuery()
                .eq(AiAutomationTaskDraft::getId, id)
                .eq(AiAutomationTaskDraft::getProjectId, projectId));
    }

    public List<AiAutomationTaskDraft> findAllByProjectIdOrderByCreatedAtDesc(Long projectId) {
        return list(Wrappers.<AiAutomationTaskDraft>lambdaQuery()
                .eq(AiAutomationTaskDraft::getProjectId, projectId)
                .orderByDesc(AiAutomationTaskDraft::getCreatedAt));
    }
}
