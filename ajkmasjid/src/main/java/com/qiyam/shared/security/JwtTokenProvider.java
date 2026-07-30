package com.qiyam.shared.security;

import com.qiyam.shared.config.AppProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {
    private final SecretKey signingKey;
    private final long expirationMs;
    private final String issuer;

    public JwtTokenProvider(AppProperties props) {
        this.signingKey = Keys.hmacShaKeyFor(props.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.expirationMs = props.jwt().expirationMs();
        this.issuer = props.jwt().issuer();
    }

    public String generateToken(UUID userId, String username, String role) {
        return generateToken(userId, username, role, (Set<Integer>) null);
    }

    /**
     * @deprecated Use {@link #generateToken(UUID, String, String, Set)} for multi-mosque support.
     */
    @Deprecated
    public String generateToken(UUID userId, String username, String role, Integer mosqueId) {
        Set<Integer> mosqueIds = mosqueId != null ? Set.of(mosqueId) : null;
        return generateToken(userId, username, role, mosqueIds);
    }

    /**
     * Generate a JWT token with multiple mosque IDs.
     * {@code mosqueIds} is null for SUPER_ADMIN (unrestricted).
     */
    public String generateToken(UUID userId, String username, String role, Set<Integer> mosqueIds) {
        var now = Instant.now();
        var builder = Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("role", role)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)));

        if (mosqueIds != null && !mosqueIds.isEmpty()) {
            builder.claim("mosqueIds", mosqueIds.stream().sorted().collect(Collectors.toList()));
        }
        // SUPER_ADMIN: omit mosqueIds claim entirely → null in filter

        return builder.signWith(signingKey).compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser().verifyWith(signingKey).requireIssuer(issuer)
                .build().parseSignedClaims(token).getPayload();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(signingKey).requireIssuer(issuer)
                    .build().parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException | ExpiredJwtException |
                 UnsupportedJwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
        }
        return false;
    }

    public UUID getUserId(String token) {
        return UUID.fromString(parseToken(token).getSubject());
    }
}
