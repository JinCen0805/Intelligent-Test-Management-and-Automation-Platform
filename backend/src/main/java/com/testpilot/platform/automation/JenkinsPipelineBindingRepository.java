package com.testpilot.platform.automation;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.common.persistence.MybatisRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JenkinsPipelineBindingRepository extends MybatisRepository<JenkinsPipelineBinding, Long> {

    public JenkinsPipelineBindingRepository(JenkinsPipelineBindingMapper mapper) {
        super(mapper, JenkinsPipelineBinding.class, JenkinsPipelineBinding::getId);
    }

    public List<JenkinsPipelineBinding> findAllByProjectIdAndAutomationProjectIdOrderByCreatedAtAsc(Long projectId, Long automationProjectId) {
        return list(Wrappers.<JenkinsPipelineBinding>lambdaQuery()
                .eq(JenkinsPipelineBinding::getProjectId, projectId)
                .eq(JenkinsPipelineBinding::getAutomationProjectId, automationProjectId)
                .orderByAsc(JenkinsPipelineBinding::getCreatedAt));
    }

    public Optional<JenkinsPipelineBinding> findFirstByProjectIdAndAutomationProjectIdOrderByCreatedAtAsc(Long projectId, Long automationProjectId) {
        return findFirst(Wrappers.<JenkinsPipelineBinding>lambdaQuery()
                .eq(JenkinsPipelineBinding::getProjectId, projectId)
                .eq(JenkinsPipelineBinding::getAutomationProjectId, automationProjectId)
                .orderByAsc(JenkinsPipelineBinding::getCreatedAt));
    }

    public Optional<JenkinsPipelineBinding> findByIdAndProjectId(Long id, Long projectId) {
        return findOne(Wrappers.<JenkinsPipelineBinding>lambdaQuery()
                .eq(JenkinsPipelineBinding::getId, id)
                .eq(JenkinsPipelineBinding::getProjectId, projectId));
    }

    public Optional<JenkinsPipelineBinding> findFirstByAutomationProjectIdOrderByCreatedAtAsc(Long automationProjectId) {
        return findFirst(Wrappers.<JenkinsPipelineBinding>lambdaQuery()
                .eq(JenkinsPipelineBinding::getAutomationProjectId, automationProjectId)
                .orderByAsc(JenkinsPipelineBinding::getCreatedAt));
    }

    public Optional<JenkinsPipelineBinding> findByProjectIdAndAutomationProjectIdAndJenkinsInstanceIdAndJobName(
            Long projectId, Long automationProjectId, Long jenkinsInstanceId, String jobName) {
        return findOne(Wrappers.<JenkinsPipelineBinding>lambdaQuery()
                .eq(JenkinsPipelineBinding::getProjectId, projectId)
                .eq(JenkinsPipelineBinding::getAutomationProjectId, automationProjectId)
                .eq(JenkinsPipelineBinding::getJenkinsInstanceId, jenkinsInstanceId)
                .eq(JenkinsPipelineBinding::getJobName, jobName));
    }
}
