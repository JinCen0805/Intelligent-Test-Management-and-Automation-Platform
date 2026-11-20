package com.testpilot.platform.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.testpilot.platform.testcase.TestCaseRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiCaseNoAllocatorTest {

    private TestCaseRepository testCaseRepository;
    private AiCaseNoAllocator allocator;

    @BeforeEach
    void setUp() {
        testCaseRepository = mock(TestCaseRepository.class);
        allocator = new AiCaseNoAllocator(testCaseRepository);
    }

    @Test
    void allocatesAfterMaxSequenceInsteadOfCount() {
        // 删除了中间用例导致 count=2，旧实现会分配 MODULE_003 与已有编号冲突。
        when(testCaseRepository.findCaseNosByProjectIdAndSuiteType(1L, "FUNCTIONAL"))
                .thenReturn(List.of("MODULE_002", "MODULE_005"));

        String caseNo = allocator.allocate(1L, "FUNCTIONAL", Set.of(), "MODULE_001");

        assertEquals("MODULE_006", caseNo);
    }

    @Test
    void skipsReservedCaseNumbers() {
        when(testCaseRepository.findCaseNosByProjectIdAndSuiteType(1L, "API"))
                .thenReturn(List.of("ORDER_001", "ORDER_002"));

        String caseNo = allocator.allocate(1L, "API", Set.of("ORDER_003", "ORDER_004"), "ORDER_002");

        assertEquals("ORDER_005", caseNo);
    }

    @Test
    void fallsBackToModulePrefixWhenSeedIsInvalid() {
        when(testCaseRepository.findCaseNosByProjectIdAndSuiteType(1L, "FUNCTIONAL"))
                .thenReturn(List.of());

        assertEquals("MODULE_001", allocator.allocate(1L, "FUNCTIONAL", Set.of(), null));
    }
}
