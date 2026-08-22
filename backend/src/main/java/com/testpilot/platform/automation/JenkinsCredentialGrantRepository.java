package com.testpilot.platform.automation;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.common.persistence.MybatisRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JenkinsCredentialGrantRepository extends MybatisRepository<JenkinsCredentialGrant, Long> {

    public JenkinsCredentialGrantRepository(JenkinsCredentialGrantMapper mapper) {
        super(mapper, JenkinsCredentialGrant.class, JenkinsCredentialGrant::getId);
    }

    public List<JenkinsCredentialGrant> findAllByProjectIdAndStatusOrderByCredentialRefAsc(Long projectId, String status) {
        return list(Wrappers.<JenkinsCredentialGrant>lambdaQuery()
                .eq(JenkinsCredentialGrant::getProjectId, projectId)
                .eq(JenkinsCredentialGrant::getStatus, status)
                .orderByAsc(JenkinsCredentialGrant::getCredentialRef));
    }

    public Optional<JenkinsCredentialGrant> findByIdAndProjectId(Long id, Long projectId) {
        return findOne(Wrappers.<JenkinsCredentialGrant>lambdaQuery()
                .eq(JenkinsCredentialGrant::getId, id)
                .eq(JenkinsCredentialGrant::getProjectId, projectId));
    }

    public boolean existsByProjectIdAndCredentialRefAndStatus(Long projectId, String credentialRef, String status) {
        return exists(Wrappers.<JenkinsCredentialGrant>lambdaQuery()
                .eq(JenkinsCredentialGrant::getProjectId, projectId)
                .eq(JenkinsCredentialGrant::getCredentialRef, credentialRef)
                .eq(JenkinsCredentialGrant::getStatus, status));
    }

    public boolean existsByProjectIdAndCredentialScopeRefAndStatus(Long projectId, String credentialScopeRef, String status) {
        return exists(Wrappers.<JenkinsCredentialGrant>lambdaQuery()
                .eq(JenkinsCredentialGrant::getProjectId, projectId)
                .eq(JenkinsCredentialGrant::getCredentialScopeRef, credentialScopeRef)
                .eq(JenkinsCredentialGrant::getStatus, status));
    }

    public Optional<JenkinsCredentialGrant> findByProjectIdAndCredentialRefAndStatus(Long projectId, String credentialRef, String status) {
        return findOne(Wrappers.<JenkinsCredentialGrant>lambdaQuery()
                .eq(JenkinsCredentialGrant::getProjectId, projectId)
                .eq(JenkinsCredentialGrant::getCredentialRef, credentialRef)
                .eq(JenkinsCredentialGrant::getStatus, status));
    }
}
