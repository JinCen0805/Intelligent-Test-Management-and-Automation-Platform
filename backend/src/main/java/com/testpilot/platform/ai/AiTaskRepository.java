package com.testpilot.platform.ai;

import com.testpilot.platform.common.persistence.MybatisRepository;
import org.springframework.stereotype.Repository;

@Repository
public class AiTaskRepository extends MybatisRepository<AiTask, String> {

    public AiTaskRepository(AiTaskMapper mapper) {
        super(mapper, AiTask.class, AiTask::getId);
    }
}
