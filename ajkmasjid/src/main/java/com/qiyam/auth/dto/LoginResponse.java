package com.qiyam.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.qiyam.shared.security.Role;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Response payload returned after successful login/Google auth.
 *
 * @param memberships Per-mosque committee role + permissions. When the user
 *                    switches active mosque, the frontend derives current
 *                    permissions from this list instead of the union.
 */
/**
 * @param token     Deprecated — always null since session cookie migration.
 *                  Kept for backward compatibility with old frontends.
 * @param sessionId The opaque session UUID set as an httpOnly cookie. Excluded from the
 *                  serialized JSON body (see {@link JsonIgnore}) — it exists solely so
 *                  the controller can read it off the returned object to set the
 *                  Set‑Cookie header; duplicating it into the response body would
 *                  defeat the point of the httpOnly cookie by handing the raw session
 *                  token to any JS (or XSS) that can read the fetch response.
 */
public record LoginResponse(
        UUID userId,
        String username,
        String fullName,
        String token,
        Role role,
        boolean isSuperAdmin,
        Set<Integer> mosqueIds,
        List<String> permissions,
        List<MosqueMembership> memberships,
        @JsonIgnore String sessionId) {}
