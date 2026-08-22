package com.testpilot.platform.api;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.common.persistence.MybatisRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ApiCollectionRepository extends MybatisRepository<ApiCollection, Long> {

    public ApiCollectionRepository(ApiCollectionMapper mapper) {
        super(mapper, ApiCollection.class, ApiCollection::getId);
    }

    public List<ApiCollection> findAllByProjectIdOrderByCreatedAtAsc(Long projectId) {
        return list(Wrappers.<ApiCollection>lambdaQuery()
                .eq(ApiCollection::getProjectId, projectId)
                .orderByAsc(ApiCollection::getCreatedAt));
    }

    public Optional<ApiCollection> findByIdAndProjectId(Long id, Long projectId) {
        return findOne(Wrappers.<ApiCollection>lambdaQuery()
                .eq(ApiCollection::getId, id)
                .eq(ApiCollection::getProjectId, projectId));
    }
}
