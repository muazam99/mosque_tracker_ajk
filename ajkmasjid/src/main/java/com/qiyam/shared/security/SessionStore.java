package com.qiyam.shared.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory session store mapping session UUID → UserPrincipal.
 * Sessions are checked on every request via {@link SessionCookieFilter}.
 * Entries expire after {@code app.jwt.expiration-ms} (mirrors the session cookie's
 * max-age) and are swept periodically so abandoned sessions don't accumulate forever.
 *
 * For production, replace with Redis or a database-backed store.
 */
@Slf4j
@Component
public class SessionStore {

    private record Entry(UserPrincipal principal, Instant expiresAt) {}

    private final Map<String, Entry> sessions = new ConcurrentHashMap<>();

    @Value("${app.session.ttl-ms:86400000}")
    private long ttlMs;

    /**
     * Create a new session and return its UUID token.
     */
    public String create(UserPrincipal principal) {
        var sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, new Entry(principal, Instant.now().plusMillis(ttlMs)));
        return sessionId;
    }

    /**
     * Look up a session by its UUID token. Expired sessions are evicted and treated as absent.
     */
    public Optional<UserPrincipal> get(String sessionId) {
        var entry = sessions.get(sessionId);
        if (entry == null) return Optional.empty();
        if (entry.expiresAt().isBefore(Instant.now())) {
            sessions.remove(sessionId);
            return Optional.empty();
        }
        return Optional.of(entry.principal());
    }

    /**
     * Invalidate (remove) a session.
     */
    public void remove(String sessionId) {
        sessions.remove(sessionId);
    }

    /**
     * Return the number of active sessions (useful for monitoring).
     */
    public int size() {
        return sessions.size();
    }

    /** Periodically sweeps expired sessions so abandoned logins don't leak memory. */
    @Scheduled(fixedDelay = 600_000)
    public void evictExpired() {
        var now = Instant.now();
        var before = sessions.size();
        sessions.values().removeIf(e -> e.expiresAt().isBefore(now));
        var removed = before - sessions.size();
        if (removed > 0) {
            log.debug("Evicted {} expired session(s), {} remaining", removed, sessions.size());
        }
    }
}
