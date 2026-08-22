package com.testpilot.platform.api;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.common.persistence.MybatisRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ApiEnvironmentVariableRepository extends MybatisRepository<ApiEnvironmentVariable, Long> {

    public ApiEnvironmentVariableRepository(ApiEnvironmentVariableMapper mapper) {
        super(mapper, ApiEnvironmentVariable.class, ApiEnvironmentVariable::getId);
    }

    public List<ApiEnvironmentVariable> findAllByProjectIdAndEnvironmentOrderByNameAsc(Long projectId, String environment) {
        return list(Wrappers.<ApiEnvironmentVariable>lambdaQuery()
                .eq(ApiEnvironmentVariable::getProjectId, projectId)
                .eq(ApiEnvironmentVariable::getEnvironment, environment)
                .orderByAsc(ApiEnvironmentVariable::getName));
    }

    public Optional<ApiEnvironmentVariable> findByIdAndProjectId(Long id, Long projectId) {
        return findOne(Wrappers.<ApiEnvironmentVariable>lambdaQuery()
                .eq(ApiEnvironmentVariable::getId, id)
                .eq(ApiEnvironmentVariable::getProjectId, projectId));
    }

    public Optional<ApiEnvironmentVariable> findByProjectIdAndEnvironmentAndName(Long projectId, String environment, String name) {
        return findOne(Wrappers.<ApiEnvironmentVariable>lambdaQuery()
                .eq(ApiEnvironmentVariable::getProjectId, projectId)
                .eq(ApiEnvironmentVariable::getEnvironment, environment)
                .eq(ApiEnvironmentVariable::getName, name));
    }
}
