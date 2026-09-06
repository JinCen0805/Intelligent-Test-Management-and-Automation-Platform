package com.testpilot.platform.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public UserDetailsService unusedUserDetailsService() {
        // Kept as a placeholder because authentication is handled by the bearer filter chain.
        return username -> {
            throw new UsernameNotFoundException(username);
        };
    }

    @Bean
    public SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            BearerAuthenticationFilter bearerAuthenticationFilter,
            ApiSecurityFailureHandler failureHandler,
            @Value("${testpilot.security.http-status-enforcement:true}") boolean enforceHttpStatus,
            Environment environment
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .logout(logout -> logout.disable())
                .requestCache(requestCache -> requestCache.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                failureHandler.unauthorized(response, "未登录"))
                        .accessDeniedHandler((request, response, exception) ->
                                failureHandler.forbidden(response))
                )
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers(
                            "/api/health",
                            "/api/auth/login",
                            "/api/integrations/jenkins/callback/**",
                            "/api/reports/public/**"
                    ).permitAll();
                    boolean testCompatibility = !enforceHttpStatus && environment.acceptsProfiles(Profiles.of("test"));
                    if (!enforceHttpStatus && !testCompatibility) {
                        throw new IllegalStateException(
                                "testpilot.security.http-status-enforcement=false 仅允许在 test profile 使用");
                    }
                    // The legacy HTTP-200 envelope is test-only; every deployed profile is authenticated.
                    if (testCompatibility) {
                        authorize.anyRequest().permitAll();
                    } else {
                        authorize.anyRequest().authenticated();
                    }
                })
                .addFilterBefore(bearerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
