package com.testpilot.platform.project;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.common.persistence.MybatisRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ProjectRepository extends MybatisRepository<Project, Long> {

    public ProjectRepository(ProjectMapper mapper) {
        super(mapper, Project.class, Project::getId);
    }

    public List<Project> findAllByDeletedFalseOrderByCreatedAtDesc() {
        // deleted 过滤由实体上的 @TableLogic 自动追加，这里不再重复拼接条件。
        return list(Wrappers.<Project>lambdaQuery().orderByDesc(Project::getCreatedAt));
    }

    public Optional<Project> findByIdAndDeletedFalse(Long id) {
        return findOne(Wrappers.<Project>lambdaQuery().eq(Project::getId, id));
    }

    public Optional<Project> findByCodeAndDeletedFalse(String code) {
        return findOne(Wrappers.<Project>lambdaQuery().eq(Project::getCode, code));
    }

    public boolean existsByCode(String code) {
        return exists(Wrappers.<Project>lambdaQuery().eq(Project::getCode, code));
    }

    public long countByDeletedFalse() {
        return count(Wrappers.<Project>lambdaQuery());
    }
}
