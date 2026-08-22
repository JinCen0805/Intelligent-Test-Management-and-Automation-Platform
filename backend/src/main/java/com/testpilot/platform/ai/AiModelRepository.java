package com.testpilot.platform.ai;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.common.persistence.MybatisRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AiModelRepository extends MybatisRepository<AiModel, Long> {

    public AiModelRepository(AiModelMapper mapper) {
        super(mapper, AiModel.class, AiModel::getId);
    }

    public List<AiModel> findAllByOrderByCreatedAtDesc() {
        return list(Wrappers.<AiModel>lambdaQuery().orderByDesc(AiModel::getCreatedAt));
    }

    public Optional<AiModel> findFirstByEnabledTrueOrderByCreatedAtAsc() {
        return findFirst(Wrappers.<AiModel>lambdaQuery().eq(AiModel::isEnabled, true).orderByAsc(AiModel::getCreatedAt));
    }
}
