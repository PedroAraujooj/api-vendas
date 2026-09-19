package com.exemplo.clientesservice.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .oauth2ResourceServer(resource -> resource.jwt(jwt -> {}))
            .build();
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${auth.jwk-set-uri}") String jwks,
                          @Value("${auth.issuer}") String issuer) {
        var decoder = NimbusJwtDecoder.withJwkSetUri(jwks).build();
        OAuth2TokenValidator<Jwt> requiredClaims = jwt -> {
            boolean valid = jwt.getExpiresAt() != null && jwt.getSubject() != null && !jwt.getSubject().isBlank()
                && jwt.getAudience() != null && jwt.getAudience().contains("clientes-service")
                && "access".equals(jwt.getClaimAsString("token_type"));
            return valid ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Claims do token invalidas", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(Duration.ZERO), new JwtIssuerValidator(issuer), requiredClaims));
        return decoder;
    }
}
