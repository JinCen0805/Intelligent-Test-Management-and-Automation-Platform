package com.testpilot.platform.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
            @Value("${testpilot.security.http-status-enforcement:true}") boolean enforceHttpStatus
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
                    if (enforceHttpStatus) {
                        authorize.anyRequest().authenticated();
                    } else {
                        // The compatibility switch keeps the stage-0 HTTP 200 business envelope
                        // until all controllers move to SecurityContext-based authorization.
                        authorize.anyRequest().permitAll();
                    }
                })
                .addFilterBefore(bearerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
