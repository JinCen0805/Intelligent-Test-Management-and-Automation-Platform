package com.testpilot.platform.api;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.common.persistence.MybatisRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ApiRequestRepository extends MybatisRepository<ApiRequest, Long> {

    public ApiRequestRepository(ApiRequestMapper mapper) {
        super(mapper, ApiRequest.class, ApiRequest::getId);
    }

    public List<ApiRequest> findAllByCollectionIdOrderByCreatedAtAsc(Long collectionId) {
        return list(Wrappers.<ApiRequest>lambdaQuery()
                .eq(ApiRequest::getCollectionId, collectionId)
                .orderByAsc(ApiRequest::getCreatedAt));
    }

    public List<ApiRequest> findAllByProjectId(Long projectId) {
        return list(Wrappers.<ApiRequest>lambdaQuery().eq(ApiRequest::getProjectId, projectId));
    }

    public Optional<ApiRequest> findByIdAndProjectId(Long id, Long projectId) {
        return findOne(Wrappers.<ApiRequest>lambdaQuery()
                .eq(ApiRequest::getId, id)
                .eq(ApiRequest::getProjectId, projectId));
    }
}
