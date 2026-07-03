package com.qiyam.shared.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime role representation. Roles are loaded dynamically from Supabase.
 * Hierarchy: lower level number = higher privilege.
 */
public record Role(
        String name,
        int level
) {
    private static final Map<String, Role> CACHE = new ConcurrentHashMap<>();

    public static Role from(String name, int level) {
        var key = name.toUpperCase();
        return CACHE.computeIfAbsent(key, k -> new Role(k, level));
    }

    public static Role fromString(String name) {
        if (name == null) return GUEST;
        var cached = CACHE.get(name.toUpperCase());
        return cached != null ? cached : GUEST;
    }

    /**
     * Returns true if this role has equal or higher privilege than another.
     */
    public boolean isAtLeast(Role other) {
        return this.level <= other.level;
    }

    /**
     * Returns true if this role can manage the target role hierarchically.
     */
    public boolean canManage(Role other) {
        return this.level < other.level;
    }

    /** Fallback role for unauthenticated / unknown users (lowest privilege). */
    public static final Role GUEST = new Role("PUBLIC_USER", 999);
}
