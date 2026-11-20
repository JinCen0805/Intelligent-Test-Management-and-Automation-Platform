package com.testpilot.platform.automation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.project.ProjectRepository;
import com.testpilot.platform.user.User;
import com.testpilot.platform.user.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 并发上报同一执行结果时，只允许一个终态写入成功；其余必须得到 40912/40913，
 * 且最终状态与报告不能出现回退或重复。
 */
@SpringBootTest
@ActiveProfiles("test")
class TestExecutionConcurrencyTest {

    @Autowired
    private AutomationService automationService;

    @Autowired
    private TestExecutionRepository testExecutionRepository;

    @Autowired
    private TestReportRepository testReportRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void concurrentTerminalReportsOnlyOneWins() throws Exception {
        Long projectId = projectRepository.findByCodeAndDeletedFalse("XH-MALL")
                .orElseThrow()
                .getId();
        User user = userRepository.findByUsername("lin.xiao").orElseThrow();
        TestExecution execution = testExecutionRepository.save(new TestExecution(
                projectId,
                424242L,
                424242L,
                "concurrency-job",
                null,
                "42",
                "QUEUED",
                LocalDateTime.now()
        ));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch startGate = new CountDownLatch(1);
        try {
            List<Future<String>> futures = new ArrayList<>();
            futures.add(pool.submit(() -> reportResult(
                    startGate, projectId, execution.getId(), user, "PASSED", 1, 1, 0, 0, 0)));
            futures.add(pool.submit(() -> reportResult(
                    startGate, projectId, execution.getId(), user, "FAILED", 1, 0, 1, 0, 0)));
            startGate.countDown();

            List<String> outcomes = new ArrayList<>();
            for (Future<String> future : futures) {
                outcomes.add(future.get(30, TimeUnit.SECONDS));
            }

            long successes = outcomes.stream().filter("OK"::equals).count();
            long conflicts = outcomes.stream().filter(outcome -> outcome.startsWith("409")).count();
            assertEquals(1, successes, "只能有一个终态写入成功，实际: " + outcomes);
            assertEquals(1, conflicts, "另一个必须得到 40912/40913，实际: " + outcomes);
        } finally {
            pool.shutdownNow();
        }

        TestExecution persisted = testExecutionRepository.findById(execution.getId()).orElseThrow();
        assertTrue(List.of("PASSED", "FAILED").contains(persisted.getStatus()),
                "终态不能回退或为空: " + persisted.getStatus());
        assertEquals(1, testReportRepository.findAllByExecutionIdIn(List.of(execution.getId())).size(),
                "同一执行只能有一条报告");
    }

    private String reportResult(
            CountDownLatch startGate,
            Long projectId,
            Long executionId,
            User user,
            String status,
            int total,
            int passed,
            int failed,
            int broken,
            int skipped
    ) throws InterruptedException {
        startGate.await();
        try {
            automationService.updateResult(
                    projectId,
                    executionId,
                    new AutomationDtos.ExecutionResultRequest(
                            status, null, null, null, total, passed, failed, broken, skipped,
                            null, null, null, null, null
                    ),
                    user
            );
            return "OK";
        } catch (BusinessException exception) {
            return String.valueOf(exception.getCode());
        }
    }
}
