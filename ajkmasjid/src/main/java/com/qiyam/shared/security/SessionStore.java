package com.qiyam.shared.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory session store mapping session UUID → UserPrincipal.
 * Sessions are checked on every request via {@link SessionCookieFilter}.
 *
 * For production, replace with Redis or a database-backed store.
 */
@Component
public class SessionStore {

    private final Map<String, UserPrincipal> sessions = new ConcurrentHashMap<>();

    /**
     * Create a new session and return its UUID token.
     */
    public String create(UserPrincipal principal) {
        var sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, principal);
        return sessionId;
    }

    /**
     * Look up a session by its UUID token.
     */
    public Optional<UserPrincipal> get(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
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
}
