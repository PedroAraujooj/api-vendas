package com.exemplo.authservice;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    Clock clock() { return Clock.systemUTC(); }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.GET, "/auth/jwks", "/auth/health").permitAll()
                .anyRequest().denyAll())
            .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, ex) -> response.sendError(401)))
            .build();
    }
}
