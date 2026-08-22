package com.testpilot.platform.security;

import com.testpilot.platform.auth.AuthService;
import com.testpilot.platform.common.BusinessException;
import com.testpilot.platform.user.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class BearerAuthenticationFilter extends OncePerRequestFilter {

    private final AuthService authService;
    private final ApiSecurityFailureHandler failureHandler;
    private final boolean enforceHttpStatus;

    public BearerAuthenticationFilter(
            AuthService authService,
            ApiSecurityFailureHandler failureHandler,
            @Value("${testpilot.security.http-status-enforcement:true}") boolean enforceHttpStatus
    ) {
        this.authService = authService;
        this.failureHandler = failureHandler;
        this.enforceHttpStatus = enforceHttpStatus;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            User user = authService.currentUser(authorization);
            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(
                            user,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()))
                    );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (BusinessException exception) {
            SecurityContextHolder.clearContext();
            if (enforceHttpStatus) {
                failureHandler.unauthorized(response, exception.getMessage());
                return;
            }
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
