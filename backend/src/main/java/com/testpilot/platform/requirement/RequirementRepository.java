package com.testpilot.platform.requirement;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.common.persistence.MybatisRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class RequirementRepository extends MybatisRepository<Requirement, Long> {

    public RequirementRepository(RequirementMapper mapper) {
        super(mapper, Requirement.class, Requirement::getId);
    }

    public List<Requirement> findAllByProjectIdOrderByCreatedAtDesc(Long projectId) {
        return list(Wrappers.<Requirement>lambdaQuery()
                .eq(Requirement::getProjectId, projectId)
                .orderByDesc(Requirement::getCreatedAt));
    }

    public Optional<Requirement> findByIdAndProjectId(Long id, Long projectId) {
        return findOne(Wrappers.<Requirement>lambdaQuery()
                .eq(Requirement::getId, id)
                .eq(Requirement::getProjectId, projectId));
    }
}
