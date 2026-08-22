package com.testpilot.platform.project;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.common.persistence.MybatisRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ProjectMemberRepository extends MybatisRepository<ProjectMember, Long> {

    public ProjectMemberRepository(ProjectMemberMapper mapper) {
        super(mapper, ProjectMember.class, ProjectMember::getId);
    }

    public Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId) {
        return findOne(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId));
    }

    public List<ProjectMember> findAllByProjectIdOrderByJoinedAtAsc(Long projectId) {
        return list(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .orderByAsc(ProjectMember::getJoinedAt));
    }

    public long countByProjectId(Long projectId) {
        return count(Wrappers.<ProjectMember>lambdaQuery().eq(ProjectMember::getProjectId, projectId));
    }

    public boolean existsByProjectIdAndUserId(Long projectId, Long userId) {
        return exists(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId));
    }

    public void deleteByProjectIdAndUserId(Long projectId, Long userId) {
        delete(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId));
    }
}
