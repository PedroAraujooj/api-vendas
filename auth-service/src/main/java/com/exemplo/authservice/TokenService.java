package com.exemplo.authservice;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
    private final RSAKey key;
    private final JwtEncoder encoder;
    private final Clock clock;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String passwordHash;
    private final String issuer;
    private final String audience;
    private final long accessSeconds;
    private final long refreshSeconds;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, RefreshSession> refreshSessions = new ConcurrentHashMap<>();

    public TokenService(Clock clock, PasswordEncoder passwordEncoder,
            @Value("${auth.username}") String username, @Value("${auth.password}") String password,
            @Value("${auth.issuer}") String issuer, @Value("${auth.audience}") String audience,
            @Value("${auth.access-token-seconds}") long accessSeconds,
            @Value("${auth.refresh-token-seconds}") long refreshSeconds) throws Exception {
        if (accessSeconds <= 0 || refreshSeconds <= 0) {
            throw new IllegalArgumentException("Os tempos de expiracao devem ser positivos");
        }
        this.clock = clock;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.passwordHash = passwordEncoder.encode(password);
        this.issuer = issuer;
        this.audience = audience;
        this.accessSeconds = accessSeconds;
        this.refreshSeconds = refreshSeconds;
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var pair = generator.generateKeyPair();
        key = new RSAKey.Builder((RSAPublicKey) pair.getPublic())
            .privateKey((RSAPrivateKey) pair.getPrivate()).keyID(UUID.randomUUID().toString()).build();
        encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }

    public TokenResponse login(String username, String password) {
        boolean validPassword = passwordEncoder.matches(password, passwordHash);
        if (!this.username.equals(username) || !validPassword) {
            throw new BadCredentialsException("Usuario ou senha invalidos");
        }
        return issue(username);
    }

    public TokenResponse refresh(String refreshToken) {
        // remove() e atomico: apenas uma requisicao pode consumir o mesmo token.
        var session = refreshSessions.remove(hash(refreshToken));
        if (session == null || !session.expiresAt().isAfter(clock.instant())) {
            throw new BadCredentialsException("Refresh token invalido, expirado ou ja utilizado");
        }
        return issue(session.subject());
    }

    public Map<String, Object> publicKeys() {
        return new JWKSet(key.toPublicJWK()).toJSONObject();
    }

    private TokenResponse issue(String subject) {
        Instant now = clock.instant();
        refreshSessions.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        var claims = JwtClaimsSet.builder().issuer(issuer).subject(subject)
            .audience(List.of(audience)).issuedAt(now).expiresAt(now.plusSeconds(accessSeconds))
            .id(UUID.randomUUID().toString()).claim("token_type", "access").build();
        var header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(key.getKeyID()).build();
        String accessToken = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        // Guardamos apenas o hash do refresh token, nunca o valor enviado ao usuario.
        refreshSessions.put(hash(refreshToken), new RefreshSession(subject, now.plusSeconds(refreshSeconds)));
        return new TokenResponse(accessToken, refreshToken, "Bearer", accessSeconds, refreshSeconds);
    }

    private static String hash(String token) {
        try {
            return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private record RefreshSession(String subject, Instant expiresAt) { }
    public record TokenResponse(String access_token, String refresh_token, String token_type,
                                long expires_in, long refresh_expires_in) { }
}
