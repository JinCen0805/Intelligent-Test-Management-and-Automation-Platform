package com.testpilot.platform.automation;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.common.persistence.MybatisRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AutomationProjectRepository extends MybatisRepository<AutomationProject, Long> {

    public AutomationProjectRepository(AutomationProjectMapper mapper) {
        super(mapper, AutomationProject.class, AutomationProject::getId);
    }

    public List<AutomationProject> findAllByProjectIdOrderByCreatedAtDesc(Long projectId) {
        return list(Wrappers.<AutomationProject>lambdaQuery()
                .eq(AutomationProject::getProjectId, projectId)
                .orderByDesc(AutomationProject::getCreatedAt));
    }

    public Optional<AutomationProject> findByIdAndProjectId(Long id, Long projectId) {
        return findOne(Wrappers.<AutomationProject>lambdaQuery()
                .eq(AutomationProject::getId, id)
                .eq(AutomationProject::getProjectId, projectId));
    }
}
