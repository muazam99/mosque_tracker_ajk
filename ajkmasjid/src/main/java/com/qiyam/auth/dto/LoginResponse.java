package com.qiyam.auth.dto;

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
 * @param sessionId The opaque session UUID set as an httpOnly cookie.
 *                  Frontend does NOT read this; it's only for the response
 *                  so the controller can set the Set‑Cookie header.
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
        String sessionId) {}
