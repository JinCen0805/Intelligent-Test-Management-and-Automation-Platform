package com.testpilot.platform.testcase;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 测试用例编号分配。
 *
 * <p>历史实现用 "count + 1" 生成编号：删除中间用例后 count 变小，会把已存在的编号再分配
 * 一次，触发唯一键冲突（500）。这里改为按"已使用的最大编号 + 1"分配，并跳过任何已占用
 * 编号；显式传入的编号保持原样（查重由调用方负责）。
 */
@Component
public class CaseNoAllocator {

    private static final Pattern NUMERIC_CASE_NO = Pattern.compile("^TC-(\\d+)$");
    private static final String FORMAT = "TC-%04d";

    private final TestCaseRepository testCaseRepository;

    public CaseNoAllocator(TestCaseRepository testCaseRepository) {
        this.testCaseRepository = testCaseRepository;
    }

    /**
     * @return 显式编号（大写）或下一个可用编号。
     */
    public String allocate(Long projectId, String suiteType, String preferredCaseNo) {
        if (preferredCaseNo != null && !preferredCaseNo.isBlank()) {
            return preferredCaseNo.trim().toUpperCase(Locale.ROOT);
        }
        return nextAvailable(projectId, suiteType);
    }

    public String nextAvailable(Long projectId, String suiteType) {
        Set<String> used = testCaseRepository.findCaseNosByProjectIdAndSuiteType(projectId, suiteType)
                .stream()
                .filter(caseNo -> caseNo != null)
                .collect(Collectors.toSet());
        long candidate = used.stream()
                .map(NUMERIC_CASE_NO::matcher)
                .filter(Matcher::matches)
                .mapToLong(matcher -> Long.parseLong(matcher.group(1)))
                .max()
                .orElse(0L) + 1L;
        while (used.contains(format(candidate))) {
            candidate++;
        }
        return format(candidate);
    }

    private String format(long number) {
        return String.format(FORMAT, number);
    }
}
