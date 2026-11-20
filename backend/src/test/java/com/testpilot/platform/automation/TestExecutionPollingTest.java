package com.testpilot.platform.automation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TestExecutionPollingTest {

    @Autowired
    private TestExecutionRepository testExecutionRepository;

    @Test
    void pollingQueryIsLimitedAndOrderedByUpdatedAt() {
        LocalDateTime base = LocalDateTime.now().minusMinutes(5);
        for (int index = 0; index < 5; index++) {
            TestExecution execution = new TestExecution(
                    987654L,
                    987654L,
                    987654L,
                    "poll-job-" + index,
                    null,
                    null,
                    index % 2 == 0 ? "QUEUED" : "RUNNING",
                    base.plusSeconds(index)
            );
            testExecutionRepository.save(execution);
        }

        List<TestExecution> batch = testExecutionRepository.findTopNByStatusInOrderByUpdatedAtAsc(
                List.of("QUEUED", "RUNNING"), 3);

        assertEquals(3, batch.size(), "轮询必须只取 limit 条，不能全表扫描");
        for (int index = 1; index < batch.size(); index++) {
            LocalDateTime previous = batch.get(index - 1).getUpdatedAt();
            LocalDateTime current = batch.get(index).getUpdatedAt();
            assertNotNull(previous);
            assertNotNull(current);
            assertFalse(current.isBefore(previous), "批次必须按 updated_at 升序，避免饥饿");
        }
    }
}
