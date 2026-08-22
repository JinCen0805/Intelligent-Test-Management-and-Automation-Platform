package com.testpilot.platform.common.persistence;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public abstract class MybatisRepository<T, ID extends Serializable> {

    private final BaseMapper<T> mapper;
    private final Class<T> entityClass;
    private final Function<T, ID> idExtractor;

    protected MybatisRepository(BaseMapper<T> mapper, Class<T> entityClass, Function<T, ID> idExtractor) {
        this.mapper = mapper;
        this.entityClass = entityClass;
        this.idExtractor = idExtractor;
    }

    public T save(T entity) {
        ID id = idExtractor.apply(entity);
        if (id == null || mapper.selectById(id) == null) {
            invokeLifecycle(entity, "onCreate");
            mapper.insert(entity);
        } else {
            invokeLifecycle(entity, "onUpdate");
            mapper.updateById(entity);
        }
        return entity;
    }

    public Optional<T> findById(ID id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    public List<T> findAll() {
        return mapper.selectList(query());
    }

    public List<T> findAllById(Collection<ID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return mapper.selectBatchIds(ids);
    }

    public long count() {
        return mapper.selectCount(query());
    }

    public void delete(T entity) {
        ID id = idExtractor.apply(entity);
        if (id != null) {
            mapper.deleteById(id);
        }
    }

    public void deleteById(ID id) {
        mapper.deleteById(id);
    }

    public void deleteAll(Collection<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        entities.stream()
                .map(idExtractor)
                .filter(id -> id != null)
                .forEach(mapper::deleteById);
    }

    protected Optional<T> findOne(Wrapper<T> wrapper) {
        return Optional.ofNullable(mapper.selectOne(wrapper));
    }

    protected Optional<T> findFirst(LambdaQueryWrapper<T> wrapper) {
        return Optional.ofNullable(mapper.selectOne(wrapper.last("limit 1")));
    }

    protected List<T> list(Wrapper<T> wrapper) {
        return mapper.selectList(wrapper);
    }

    protected long count(Wrapper<T> wrapper) {
        return mapper.selectCount(wrapper);
    }

    protected boolean exists(Wrapper<T> wrapper) {
        return count(wrapper) > 0;
    }

    protected void delete(Wrapper<T> wrapper) {
        mapper.delete(wrapper);
    }

    protected Page<T> page(Pageable pageable, LambdaQueryWrapper<T> wrapper) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> mpPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                        pageable.getPageNumber() + 1L,
                        pageable.getPageSize()
                );
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> selected = mapper.selectPage(mpPage, wrapper);
        return new PageImpl<>(selected.getRecords(), pageable, selected.getTotal());
    }

    protected LambdaQueryWrapper<T> query() {
        return Wrappers.lambdaQuery(entityClass);
    }

    private void invokeLifecycle(T entity, String methodName) {
        try {
            Method method = entity.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(entity);
        } catch (NoSuchMethodException ignored) {
            // Entity has no lifecycle hook.
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to invoke entity lifecycle method: " + methodName, ex);
        }
    }
}
