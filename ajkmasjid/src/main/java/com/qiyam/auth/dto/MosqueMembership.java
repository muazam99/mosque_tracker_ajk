package com.qiyam.auth.dto;

import java.util.List;

/**
 * A user's committee role and permissions at a specific mosque.
 * One user can have multiple memberships across different mosques.
 */
public record MosqueMembership(
        int mosqueId,
        String roleName,
        List<String> permissions) {}
