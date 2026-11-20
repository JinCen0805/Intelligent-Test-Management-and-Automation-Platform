package com.testpilot.platform.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.testpilot.platform.ai.AiCallAuditRepository;
import com.testpilot.platform.audit.OperationAuditRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.slf4j.MDC;

class RetentionAndRequestIdTest {

    @Test
    void requestIdIsGeneratedAndPropagated() throws Exception {
        RequestIdFilter filter = new RequestIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        String requestId = response.getHeader(RequestIdFilter.HEADER);
        assertNotNull(requestId);
        assertTrue(requestId.length() > 10);
        assertNull(MDC.get(RequestIdFilter.MDC_KEY), "MDC 必须在请求结束后清理");
    }

    @Test
    void requestIdHeaderIsPreservedWhenProvided() throws Exception {
        RequestIdFilter filter = new RequestIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        request.addHeader(RequestIdFilter.HEADER, "trace-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals("trace-123", response.getHeader(RequestIdFilter.HEADER));
    }

    @Test
    void retentionCleanupUsesConfiguredCutoff() {
        OperationAuditRepository operationRepository = mock(OperationAuditRepository.class);
        AiCallAuditRepository aiRepository = mock(AiCallAuditRepository.class);
        when(operationRepository.deleteByCreatedAtBefore(any())).thenReturn(3L);
        when(aiRepository.deleteByCreatedAtBefore(any())).thenReturn(5L);
        RetentionCleanupService service = new RetentionCleanupService(operationRepository, aiRepository, true, 90, 180);

        RetentionCleanupService.CleanupResult result = service.cleanupNow();

        assertEquals(3L, result.operationAudits());
        assertEquals(5L, result.aiCallAudits());
        ArgumentCaptor<LocalDateTime> operationCutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(operationRepository).deleteByCreatedAtBefore(operationCutoff.capture());
        assertTrue(operationCutoff.getValue().isBefore(LocalDateTime.now().minusDays(89)));
        assertTrue(operationCutoff.getValue().isAfter(LocalDateTime.now().minusDays(91)));
    }

    @Test
    void retentionCleanupCanBeDisabled() {
        OperationAuditRepository operationRepository = mock(OperationAuditRepository.class);
        AiCallAuditRepository aiRepository = mock(AiCallAuditRepository.class);
        RetentionCleanupService service = new RetentionCleanupService(operationRepository, aiRepository, false, 90, 180);

        RetentionCleanupService.CleanupResult result = service.cleanupNow();

        assertEquals(0L, result.operationAudits());
        assertEquals(0L, result.aiCallAudits());
        verify(operationRepository, never()).deleteByCreatedAtBefore(any());
        verify(aiRepository, never()).deleteByCreatedAtBefore(any());
    }
}
