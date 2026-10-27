package org.mato.command.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 6.4.3 configuration. The vulnerable timing-mitigation path
 * lives in {@link DaoAuthenticationProvider#additionalAuthenticationChecks}:
 * for passwords longer than 72 bytes the encoder throws BEFORE the dummy hash
 * runs, so an absent user returns in ~2ms while a present user takes BCrypt
 * runtime (~250ms at cost 10) — leaking user existence.
 *
 * We deliberately do NOT override or work around this — that's the CVE.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public UserDetailsService userDetailsService(@Value("${auth.president-password}") String presidentPw,
                                                 PasswordEncoder encoder) {
        var manager = new InMemoryUserDetailsManager();
        manager.createUser(User.withUsername("mr.president")
                .password(encoder.encode(presidentPw))
                .roles("ADMIN")
                .build());
        return manager;
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService uds, PasswordEncoder encoder) {
        // The login controller bypasses the AuthenticationManager so the
        // timing leak is observable without needing to defang Spring
        // Security's final retrieveUser hook. We still wire one up so the
        // Spring Security context is fully realistic.
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(uds);
        p.setPasswordEncoder(encoder);
        return new ProviderManager(p);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(c -> c.disable())
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/login", "/verify-otp", "/warheads",
                                         "/actuator/health", "/actuator/info",
                                         "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        .anyRequest().permitAll())
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .build();
    }
}
