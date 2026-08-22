package com.testpilot.platform.api;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.common.persistence.MybatisRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class ApiExecutionHistoryRepository extends MybatisRepository<ApiExecutionHistory, Long> {

    public ApiExecutionHistoryRepository(ApiExecutionHistoryMapper mapper) {
        super(mapper, ApiExecutionHistory.class, ApiExecutionHistory::getId);
    }

    public List<ApiExecutionHistory> findAllByProjectIdOrderByStartedAtDesc(Long projectId) {
        return list(Wrappers.<ApiExecutionHistory>lambdaQuery()
                .eq(ApiExecutionHistory::getProjectId, projectId)
                .orderByDesc(ApiExecutionHistory::getStartedAt));
    }

    public List<ApiExecutionHistory> findAllByRequestId(Long requestId) {
        return list(Wrappers.<ApiExecutionHistory>lambdaQuery().eq(ApiExecutionHistory::getRequestId, requestId));
    }

    public Page<ApiExecutionHistory> findAllByProjectIdOrderByStartedAtDesc(Long projectId, Pageable pageable) {
        return page(pageable, Wrappers.<ApiExecutionHistory>lambdaQuery()
                .eq(ApiExecutionHistory::getProjectId, projectId)
                .orderByDesc(ApiExecutionHistory::getStartedAt));
    }
}
