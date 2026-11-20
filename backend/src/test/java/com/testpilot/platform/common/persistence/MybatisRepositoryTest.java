package com.testpilot.platform.common.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.testpilot.platform.testcase.TestCase;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * save() 必须区分"新建（自增 id 为空）"与"更新"，且新建路径不再多查一次。
 */
class MybatisRepositoryTest {

    private BaseMapper<TestCase> mapper;
    private MybatisRepository<TestCase, Long> repository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        mapper = mock(BaseMapper.class);
        repository = new MybatisRepository<>(mapper, TestCase.class, TestCase::getId) {
        };
    }

    @Test
    void insertsWithoutSelectingWhenIdIsNull() {
        TestCase entity = sampleCase();
        when(mapper.insert(any(TestCase.class))).thenReturn(1);

        repository.save(entity);

        verify(mapper, never()).selectById(any());
        verify(mapper).insert(entity);
        verify(mapper, never()).updateById(any(TestCase.class));
    }

    @Test
    void insertsWhenIdPresentButRowDoesNotExist() throws Exception {
        TestCase entity = sampleCase();
        setField(entity, "id", 99L);
        when(mapper.selectById(99L)).thenReturn(null);
        when(mapper.insert(any(TestCase.class))).thenReturn(1);

        repository.save(entity);

        verify(mapper).selectById(99L);
        verify(mapper).insert(entity);
        verify(mapper, never()).updateById(any(TestCase.class));
    }

    @Test
    void updatesWhenRowExists() throws Exception {
        TestCase entity = sampleCase();
        setField(entity, "id", 100L);
        when(mapper.selectById(100L)).thenReturn(entity);
        when(mapper.updateById(any(TestCase.class))).thenReturn(1);

        repository.save(entity);

        verify(mapper).selectById(100L);
        verify(mapper).updateById(entity);
        verify(mapper, never()).insert(any(TestCase.class));
    }

    @Test
    void findAllUsesEmptyWrapperForAutoIdEntities() {
        repository.findAll();
        verify(mapper).selectList(any());
    }

    private TestCase sampleCase() {
        return new TestCase(
                1L, null, null, "标题", null, null, null, null, null,
                "P1", "FUNCTIONAL", "FUNCTIONAL", null, "DRAFT", "MANUAL", 1L, null
        );
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
