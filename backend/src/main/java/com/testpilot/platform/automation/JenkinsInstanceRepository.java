package com.testpilot.platform.automation;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.common.persistence.MybatisRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JenkinsInstanceRepository extends MybatisRepository<JenkinsInstance, Long> {

    public JenkinsInstanceRepository(JenkinsInstanceMapper mapper) {
        super(mapper, JenkinsInstance.class, JenkinsInstance::getId);
    }

    public List<JenkinsInstance> findAllByOrderByCreatedAtDesc() {
        return list(Wrappers.<JenkinsInstance>lambdaQuery().orderByDesc(JenkinsInstance::getCreatedAt));
    }

    public Optional<JenkinsInstance> findByIdAndStatus(Long id, String status) {
        return findOne(Wrappers.<JenkinsInstance>lambdaQuery()
                .eq(JenkinsInstance::getId, id)
                .eq(JenkinsInstance::getStatus, status));
    }
}
