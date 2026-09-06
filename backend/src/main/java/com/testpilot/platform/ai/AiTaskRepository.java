package com.testpilot.platform.ai;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.common.persistence.MybatisRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class AiTaskRepository extends MybatisRepository<AiTask, String> {

    public AiTaskRepository(AiTaskMapper mapper) {
        super(mapper, AiTask.class, AiTask::getId);
    }

    public List<AiTask> findAllRunning() {
        return list(Wrappers.<AiTask>lambdaQuery().eq(AiTask::getStatus, "RUNNING"));
    }
}
