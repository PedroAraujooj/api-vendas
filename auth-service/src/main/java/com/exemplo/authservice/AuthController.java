package com.exemplo.authservice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final TokenService tokens;

    public AuthController(TokenService tokens) { this.tokens = tokens; }

    @PostMapping("/login")
    public ResponseEntity<TokenService.TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(tokens.login(request.username(), request.password()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenService.TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(tokens.refresh(request.refresh_token()));
    }

    @GetMapping("/jwks")
    public Map<String, Object> jwks() { return tokens.publicKeys(); }

    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "UP"); }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> unauthorized(BadCredentialsException ex) {
        return ResponseEntity.status(401).cacheControl(CacheControl.noStore())
            .body(Map.of("error", "unauthorized", "message", ex.getMessage()));
    }

    public record LoginRequest(@NotBlank @Size(max = 100) String username,
                               @NotBlank @Size(max = 72) String password) { }
    public record RefreshRequest(@NotBlank @Size(max = 2048) String refresh_token) { }
}
