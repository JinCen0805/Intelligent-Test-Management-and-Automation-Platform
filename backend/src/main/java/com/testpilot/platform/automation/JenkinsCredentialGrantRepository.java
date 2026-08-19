package com.testpilot.platform.automation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JenkinsCredentialGrantRepository extends JpaRepository<JenkinsCredentialGrant, Long> {
    List<JenkinsCredentialGrant> findAllByProjectIdAndStatusOrderByCredentialRefAsc(Long projectId, String status);
    Optional<JenkinsCredentialGrant> findByIdAndProjectId(Long id, Long projectId);
    boolean existsByProjectIdAndCredentialRefAndStatus(
            Long projectId, String credentialRef, String status);
    boolean existsByProjectIdAndCredentialScopeRefAndStatus(
            Long projectId, String credentialScopeRef, String status);
    Optional<JenkinsCredentialGrant> findByProjectIdAndCredentialRefAndStatus(
            Long projectId, String credentialRef, String status);
}
