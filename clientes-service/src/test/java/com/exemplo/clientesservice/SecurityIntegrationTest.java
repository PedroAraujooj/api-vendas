package com.exemplo.clientesservice;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.cloud.config.enabled=false", "eureka.client.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:securitytest", "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
class SecurityIntegrationTest {
    private static final RSAKey KEY;
    private static final HttpServer JWKS;
    static {
        try {
            KEY = new RSAKeyGenerator(2048).keyID("test-key").generate();
            JWKS = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            JWKS.createContext("/jwks", exchange -> {
                byte[] body = new JWKSet(KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (var output = exchange.getResponseBody()) { output.write(body); }
            });
            JWKS.start();
        } catch (Exception ex) { throw new ExceptionInInitializerError(ex); }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("auth.jwk-set-uri", () -> "http://127.0.0.1:" + JWKS.getAddress().getPort() + "/jwks");
        registry.add("auth.issuer", () -> "http://auth-service");
    }

    @AfterAll
    static void stopJwks() { JWKS.stop(0); }

    @Autowired MockMvc mvc;

    @Test
    void missingMalformedAndOpaqueRefreshTokensAreRejected() throws Exception {
        mvc.perform(get("/clientes")).andExpect(status().isUnauthorized());
        for (String token : List.of("invalido", "token-opaco-de-refresh")) {
            mvc.perform(get("/clientes").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void validSignedAccessTokenReturnsClients() throws Exception {
        mvc.perform(get("/clientes").header("Authorization", "Bearer " + token(KEY, "http://auth-service", "clientes-service", "access", 300)))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].nome").value("Ana Souza"));
    }

    @Test
    void expiredWrongIssuerWrongAudienceAndWrongTypeAreRejected() throws Exception {
        for (String token : List.of(
                token(KEY, "http://auth-service", "clientes-service", "access", -1),
                token(KEY, "http://outro", "clientes-service", "access", 300),
                token(KEY, "http://auth-service", null, "access", 300),
                token(KEY, "http://auth-service", "outro-service", "access", 300),
                token(KEY, "http://auth-service", "clientes-service", "refresh", 300))) {
            mvc.perform(get("/clientes").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void forgedSignatureIsRejected() throws Exception {
        RSAKey attackerKey = new RSAKeyGenerator(2048).keyID("test-key").generate();
        mvc.perform(get("/clientes").header("Authorization", "Bearer " + token(attackerKey, "http://auth-service", "clientes-service", "access", 300)))
            .andExpect(status().isUnauthorized());
    }

    private String token(RSAKey key, String issuer, String audience, String type, long expiry) {
        Instant now = Instant.now();
        var builder = JwtClaimsSet.builder().issuer(issuer).subject("aluno")
            .issuedAt(now.minusSeconds(60)).expiresAt(now.plusSeconds(expiry)).claim("token_type", type);
        if (audience != null) { builder.audience(List.of(audience)); }
        var claims = builder.build();
        var encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
        return encoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(SignatureAlgorithm.RS256).keyId(key.getKeyID()).build(), claims)).getTokenValue();
    }
}
