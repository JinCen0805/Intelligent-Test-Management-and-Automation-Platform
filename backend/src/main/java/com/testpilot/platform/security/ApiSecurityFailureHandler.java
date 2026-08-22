package com.testpilot.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testpilot.platform.common.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class ApiSecurityFailureHandler {

    private final ObjectMapper objectMapper;

    public ApiSecurityFailureHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void unauthorized(HttpServletResponse response, String message) throws IOException {
        write(response, HttpServletResponse.SC_UNAUTHORIZED, 40100,
                message == null || message.isBlank() ? "未登录" : message);
    }

    public void forbidden(HttpServletResponse response) throws IOException {
        write(response, HttpServletResponse.SC_FORBIDDEN, 40300, "没有权限");
    }

    private void write(HttpServletResponse response, int status, int code, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.failure(code, message)));
    }
}
