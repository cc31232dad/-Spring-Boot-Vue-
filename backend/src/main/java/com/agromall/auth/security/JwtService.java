package com.agromall.auth.security;

import com.agromall.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration accessTokenTtl;
    private final Clock clock;

    @Autowired
    public JwtService(
            @Value("${agromall.jwt.secret}") String secret,
            @Value("${agromall.jwt.access-token-minutes:30}") long accessTokenMinutes
    ) {
        this(secret, Duration.ofMinutes(accessTokenMinutes), Clock.systemUTC());
    }

    JwtService(String secret, Duration accessTokenTtl, Clock clock) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 256 bits");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.accessTokenTtl = accessTokenTtl;
        this.clock = clock;
    }

    public String issue(User user, Set<String> roles) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(key)
                .compact();
    }

    public JwtPrincipal parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        return new JwtPrincipal(userId, username, Set.copyOf(roles));
    }

    public record JwtPrincipal(Long userId, String username, Set<String> roles) {
    }
}
