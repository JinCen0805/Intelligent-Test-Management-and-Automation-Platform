package com.testpilot.platform.automation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.testpilot.platform.project.ProjectAccessService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

/**
 * Allure 报告是构建产物：必须以 CSP sandbox 提供，避免其中的脚本读取平台同源存储。
 */
class AllureReportSecurityTest {

    @Test
    void reportResponsesCarrySandboxCspAndNosniff() {
        AllureReportStorage storage = mock(AllureReportStorage.class);
        AllureAccessTokenService tokenService = mock(AllureAccessTokenService.class);
        Resource resource = new ByteArrayResource("<html></html>".getBytes()) {
            @Override
            public String getFilename() {
                return "index.html";
            }
        };
        when(tokenService.verify("token-1")).thenReturn(9L);
        when(storage.reportResource(9L, null)).thenReturn(resource);

        AllureReportController controller = new AllureReportController(
                mock(TestExecutionRepository.class),
                mock(ProjectAccessService.class),
                storage,
                tokenService
        );

        ResponseEntity<Resource> response = controller.view("token-1", null);

        assertNotNull(response.getHeaders().getFirst("Content-Security-Policy"));
        assertEquals(
                "sandbox allow-scripts allow-popups allow-forms",
                response.getHeaders().getFirst("Content-Security-Policy")
        );
        assertEquals("nosniff", response.getHeaders().getFirst("X-Content-Type-Options"));
    }
}
