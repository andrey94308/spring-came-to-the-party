package org.mato.archive.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // /users is gated by the upstream gateway's debug-trace flag — by
                        // design the archive-service trusts the X-Debug-Trace header set
                        // by the edge-gateway.
                        .requestMatchers("/users", "/users/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // OpenAPI / Swagger UI for human inspection of the archive API.
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(b -> {})
                .build();
    }
}
