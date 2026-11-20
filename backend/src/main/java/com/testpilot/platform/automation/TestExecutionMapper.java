package com.testpilot.platform.automation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TestExecutionMapper extends BaseMapper<TestExecution> {

    /**
     * 条件更新执行结果：只有仍处于 QUEUED/RUNNING 的执行才允许写入终态，避免轮询/回调并发
     * 用陈旧实体覆盖已完成状态。返回受影响行数，0 表示状态已被其他线程改变。
     */
    @Update({
            "UPDATE test_executions SET",
            "  status = #{status},",
            "  build_number = COALESCE(#{buildNumber}, build_number),",
            "  queue_url = COALESCE(#{queueUrl}, queue_url),",
            "  commit_sha = COALESCE(#{commitSha}, commit_sha),",
            "  console_log_ref = COALESCE(#{consoleLogRef}, console_log_ref),",
            "  started_at = COALESCE(#{startedAt}, started_at),",
            "  finished_at = #{finishedAt},",
            "  duration_ms = COALESCE(#{durationMs}, duration_ms),",
            "  sync_failure_message = NULL,",
            "  sync_failure_count = 0,",
            "  updated_at = #{updatedAt}",
            "WHERE id = #{id} AND status IN ('QUEUED','RUNNING')"
    })
    int updateResultFromActive(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("buildNumber") String buildNumber,
            @Param("queueUrl") String queueUrl,
            @Param("commitSha") String commitSha,
            @Param("consoleLogRef") String consoleLogRef,
            @Param("startedAt") LocalDateTime startedAt,
            @Param("finishedAt") LocalDateTime finishedAt,
            @Param("durationMs") Long durationMs,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    /**
     * 条件更新运行中进度（build/queue/时间等），不改变终态；已终态时返回 0。
     */
    @Update({
            "UPDATE test_executions SET",
            "  build_number = COALESCE(#{buildNumber}, build_number),",
            "  queue_url = COALESCE(#{queueUrl}, queue_url),",
            "  commit_sha = COALESCE(#{commitSha}, commit_sha),",
            "  console_log_ref = COALESCE(#{consoleLogRef}, console_log_ref),",
            "  started_at = COALESCE(#{startedAt}, started_at),",
            "  status = 'RUNNING',",
            "  updated_at = #{updatedAt}",
            "WHERE id = #{id} AND status IN ('QUEUED','RUNNING')"
    })
    int updateProgressIfActive(
            @Param("id") Long id,
            @Param("buildNumber") String buildNumber,
            @Param("queueUrl") String queueUrl,
            @Param("commitSha") String commitSha,
            @Param("consoleLogRef") String consoleLogRef,
            @Param("startedAt") LocalDateTime startedAt,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    record ReportAggregate(Long total, Long passed, Long failed, Long broken, Long skipped, Long executions) {}
    class ReportTrend {
        private LocalDate day;
        private Long total;
        private Long passed;
        private Long failed;
        private Long broken;
        private Long skipped;
        public LocalDate getDay() { return day; }
        public void setDay(LocalDate day) { this.day = day; }
        public Long getTotal() { return total; }
        public void setTotal(Long total) { this.total = total; }
        public Long getPassed() { return passed; }
        public void setPassed(Long passed) { this.passed = passed; }
        public Long getFailed() { return failed; }
        public void setFailed(Long failed) { this.failed = failed; }
        public Long getBroken() { return broken; }
        public void setBroken(Long broken) { this.broken = broken; }
        public Long getSkipped() { return skipped; }
        public void setSkipped(Long skipped) { this.skipped = skipped; }
    }

    @Select({"<script>",
            "SELECT COALESCE(SUM(r.total_count),0) total, COALESCE(SUM(r.passed_count),0) passed,",
            "COALESCE(SUM(r.failed_count),0) failed, COALESCE(SUM(r.broken_count),0) broken,",
            "COALESCE(SUM(r.skipped_count),0) skipped, COUNT(e.id) executions",
            "FROM test_executions e LEFT JOIN test_reports r ON r.execution_id=e.id",
            "WHERE e.project_id IN <foreach collection='projectIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "AND e.created_at BETWEEN #{from} AND #{to}",
            "<if test='status != null'>AND e.status=#{status}</if>",
            "</script>"})
    ReportAggregate aggregateReports(@Param("projectIds") List<Long> projectIds,
                                     @Param("status") String status,
                                     @Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to);

    @Results(id = "reportTrendMapping", value = {
            @Result(property = "day", column = "trend_day"),
            @Result(property = "total", column = "total"),
            @Result(property = "passed", column = "passed"),
            @Result(property = "failed", column = "failed"),
            @Result(property = "broken", column = "broken"),
            @Result(property = "skipped", column = "skipped")
    })
    @Select({"<script>",
            "SELECT DATE(e.created_at) trend_day, COALESCE(SUM(r.total_count),0) total,",
            "COALESCE(SUM(r.passed_count),0) passed, COALESCE(SUM(r.failed_count),0) failed,",
            "COALESCE(SUM(r.broken_count),0) broken, COALESCE(SUM(r.skipped_count),0) skipped",
            "FROM test_executions e LEFT JOIN test_reports r ON r.execution_id=e.id",
            "WHERE e.project_id IN <foreach collection='projectIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "AND e.created_at BETWEEN #{from} AND #{to}",
            "<if test='status != null'>AND e.status=#{status}</if>",
            "GROUP BY DATE(e.created_at) ORDER BY DATE(e.created_at)",
            "</script>"})
    List<ReportTrend> trendReports(@Param("projectIds") List<Long> projectIds,
                                   @Param("status") String status,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);
}
