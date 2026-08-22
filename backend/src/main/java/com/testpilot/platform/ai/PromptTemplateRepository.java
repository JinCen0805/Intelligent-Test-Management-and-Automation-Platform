package com.testpilot.platform.ai;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.common.persistence.MybatisRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PromptTemplateRepository extends MybatisRepository<PromptTemplate, Long> {

    public PromptTemplateRepository(PromptTemplateMapper mapper) {
        super(mapper, PromptTemplate.class, PromptTemplate::getId);
    }

    public List<PromptTemplate> findAllByOrderByCreatedAtDesc() {
        return list(Wrappers.<PromptTemplate>lambdaQuery().orderByDesc(PromptTemplate::getCreatedAt));
    }

    public Optional<PromptTemplate> findFirstByTaskTypeAndEnabledTrueOrderByCreatedAtDesc(String taskType) {
        return findFirst(Wrappers.<PromptTemplate>lambdaQuery()
                .eq(PromptTemplate::getTaskType, taskType)
                .eq(PromptTemplate::isEnabled, true)
                .orderByDesc(PromptTemplate::getCreatedAt));
    }

    public List<PromptTemplate> findByNameAndTaskType(String name, String taskType) {
        return list(Wrappers.<PromptTemplate>lambdaQuery()
                .eq(PromptTemplate::getName, name)
                .eq(PromptTemplate::getTaskType, taskType));
    }
}
