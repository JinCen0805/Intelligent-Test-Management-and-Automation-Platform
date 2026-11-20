package com.testpilot.platform.ai;

import com.testpilot.platform.testcase.TestCaseRepository;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * AI 用例编号分配：按"同前缀已用最大序号 + 1"生成，而不是按条数 count+1。
 *
 * <p>删除中间用例后 count 会变小，旧实现会把已存在的编号再分配一次；并发时也可能撞唯一键。
 * 这里以最大序号为基准，并跳过 reserved/已存在编号；落库冲突由调用方重试。
 */
@Component
public class AiCaseNoAllocator {

    private final TestCaseRepository testCaseRepository;

    public AiCaseNoAllocator(TestCaseRepository testCaseRepository) {
        this.testCaseRepository = testCaseRepository;
    }

    public String allocate(Long projectId, String suiteType, Set<String> reservedCaseNos, String seedCaseNo) {
        String prefix = AiCasePayloadParser.casePrefix(seedCaseNo);
        Set<String> existing = new HashSet<>(
                testCaseRepository.findCaseNosByProjectIdAndSuiteType(projectId, suiteType));
        long next = AiCasePayloadParser.sequenceOf(seedCaseNo) + 1;
        for (String caseNo : existing) {
            if (caseNo != null && caseNo.toUpperCase(Locale.ROOT).startsWith(prefix + "_")) {
                next = Math.max(next, AiCasePayloadParser.sequenceOf(caseNo) + 1);
            }
        }
        String candidate;
        do {
            candidate = prefix + "_" + String.format("%03d", next++);
        } while (reservedCaseNos.contains(candidate) || existing.contains(candidate));
        return candidate;
    }
}
