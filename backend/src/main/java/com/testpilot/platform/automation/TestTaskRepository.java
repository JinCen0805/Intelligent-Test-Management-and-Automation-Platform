package com.testpilot.platform.automation;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.common.persistence.MybatisRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class TestTaskRepository extends MybatisRepository<TestTask, Long> {

    public TestTaskRepository(TestTaskMapper mapper) {
        super(mapper, TestTask.class, TestTask::getId);
    }

    public List<TestTask> findAllByProjectIdOrderByCreatedAtDesc(Long projectId) {
        return list(Wrappers.<TestTask>lambdaQuery()
                .eq(TestTask::getProjectId, projectId)
                .orderByDesc(TestTask::getCreatedAt));
    }

    public Optional<TestTask> findByIdAndProjectId(Long id, Long projectId) {
        return findOne(Wrappers.<TestTask>lambdaQuery()
                .eq(TestTask::getId, id)
                .eq(TestTask::getProjectId, projectId));
    }
}
