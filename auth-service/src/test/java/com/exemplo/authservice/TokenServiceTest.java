package com.exemplo.authservice;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TokenServiceTest {
    @Test
    void expiredRefreshIsRejected() throws Exception {
        Clock clock = mock(Clock.class);
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        when(clock.instant()).thenReturn(now);
        var service = new TokenService(clock, new BCryptPasswordEncoder(), "aluno", "senha", "issuer", "clientes-service", 30, 60);
        var tokens = service.login("aluno", "senha");
        when(clock.instant()).thenReturn(now.plusSeconds(60));
        assertThatThrownBy(() -> service.refresh(tokens.refresh_token())).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void concurrentRefreshCanOnlySucceedOnce() throws Exception {
        var service = new TokenService(Clock.systemUTC(), new BCryptPasswordEncoder(), "aluno", "senha", "issuer", "clientes-service", 30, 60);
        var tokens = service.login("aluno", "senha");
        var executor = Executors.newFixedThreadPool(2);
        Callable<Boolean> refresh = () -> {
            try { service.refresh(tokens.refresh_token()); return true; }
            catch (BadCredentialsException ex) { return false; }
        };
        try {
            var results = executor.invokeAll(List.of(refresh, refresh));
            assertThat(results.get(0).get() ^ results.get(1).get()).isTrue();
        } finally { executor.shutdownNow(); }
    }
}
