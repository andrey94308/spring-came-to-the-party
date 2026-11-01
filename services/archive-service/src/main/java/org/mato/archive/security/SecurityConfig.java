package org.mato.archive.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(10); }

    @Bean
    public LdapContextSource ldapContextSource(@Value("${ldap.url}") String url,
                                               @Value("${ldap.base}") String base) {
        var source = new LdapContextSource();
        source.setUrl(url);
        source.setBase(base);
        source.afterPropertiesSet();
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, LdapBindAuthenticationProvider provider)
            throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .authenticationProvider(provider)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/users", "/users/**").hasRole("ADMIN")
                        .requestMatchers("/notes", "/notes/**", "/documents/search",
                                "/documents/search-safe", "/documents/search-debug").hasRole("USER")
                        .anyRequest().denyAll())
                .httpBasic(b -> {})
                .build();
    }
}
