package com.testpilot.platform.testcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaseNoAllocatorTest {

    private TestCaseRepository testCaseRepository;
    private CaseNoAllocator allocator;

    @BeforeEach
    void setUp() {
        testCaseRepository = mock(TestCaseRepository.class);
        allocator = new CaseNoAllocator(testCaseRepository);
    }

    @Test
    void startsFromFirstNumberWhenSuiteIsEmpty() {
        when(testCaseRepository.findCaseNosByProjectIdAndSuiteType(1L, "FUNCTIONAL")).thenReturn(List.of());

        assertEquals("TC-0001", allocator.nextAvailable(1L, "FUNCTIONAL"));
    }

    @Test
    void continuesAfterHighestNumberInsteadOfCountingRows() {
        // 删掉中间编号后 count 变小，但编号必须继续从最大值往后分配。
        when(testCaseRepository.findCaseNosByProjectIdAndSuiteType(1L, "FUNCTIONAL"))
                .thenReturn(List.of("TC-0001", "TC-0003"));

        assertEquals("TC-0004", allocator.nextAvailable(1L, "FUNCTIONAL"));
    }

    @Test
    void ignoresNonNumericAndLowercaseNumbers() {
        when(testCaseRepository.findCaseNosByProjectIdAndSuiteType(1L, "API"))
                .thenReturn(List.of("TC-AI-001", "tc-0002", "TC-0005"));

        assertEquals("TC-0006", allocator.nextAvailable(1L, "API"));
    }

    @Test
    void skipsNumbersThatAreAlreadyUsed() {
        when(testCaseRepository.findCaseNosByProjectIdAndSuiteType(1L, "FUNCTIONAL"))
                .thenReturn(List.of("TC-0002", "TC-0003", "TC-0004"));

        assertEquals("TC-0005", allocator.nextAvailable(1L, "FUNCTIONAL"));
    }

    @Test
    void keepsExplicitCaseNoAndNormalizesCase() {
        assertEquals("TC-CUSTOM-01", allocator.allocate(1L, "FUNCTIONAL", " tc-custom-01 "));
    }

    @Test
    void allocatesAvailableNumberWhenNoExplicitCaseNoProvided() {
        when(testCaseRepository.findCaseNosByProjectIdAndSuiteType(1L, "API")).thenReturn(List.of("TC-0007"));

        assertEquals("TC-0008", allocator.allocate(1L, "API", null));
    }
}
