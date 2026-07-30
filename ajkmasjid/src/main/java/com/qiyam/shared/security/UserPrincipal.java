package com.qiyam.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Custom principal carrying user identity, role, and mosque affiliations.
 * SUPER_ADMIN (level 1) has {@code mosqueIds = null} (unrestricted access).
 * Regular users have a set of mosque IDs from their mosque_committees memberships.
 */
public record UserPrincipal(
        UUID userId,
        String username,
        String email,
        String fullName,
        Role role,
        Set<Integer> mosqueIds
) {

    /** Convenience constructor for backward compatibility with single mosque. */
    public UserPrincipal(UUID userId, String username, String email, String fullName, Role role, Integer mosqueId) {
        this(userId, username, email, fullName, role,
             mosqueId != null ? Set.of(mosqueId) : null);
    }

    /** Returns true if this principal has SUPER_ADMIN privileges (level 1). */
    public boolean isSuperAdmin() {
        return role.level() == 1;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public boolean hasRole(Role other) {
        return this.role.name().equals(other.name());
    }

    public boolean hasAnyRole(Role... roles) {
        for (Role r : roles) {
            if (this.role.name().equals(r.name())) return true;
        }
        return false;
    }

    public boolean isAtLeast(Role other) {
        return this.role.isAtLeast(other);
    }

    /**
     * Returns true if the user has access to the given mosque.
     * SUPER_ADMIN (level 1) bypasses all mosque restrictions.
     * Regular users must have the target mosque in their mosqueIds set.
     */
    public boolean hasMosqueAccess(Integer targetMosqueId) {
        if (isSuperAdmin()) return true;
        if (targetMosqueId == null) return true;
        return mosqueIds != null && mosqueIds.contains(targetMosqueId);
    }

    /**
     * Returns the set of mosque IDs this user has access to.
     * Returns empty set for regular users with no mosque assignments.
     */
    public Set<Integer> getMosqueIdSet() {
        if (isSuperAdmin()) return Collections.emptySet(); // Sentinel: empty = all access
        return mosqueIds != null ? mosqueIds : Collections.emptySet();
    }

    /**
     * Extract UserPrincipal from the Spring Security Authentication.
     */
    public static UserPrincipal from(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal up)) {
            return null;
        }
        return up;
    }

    /**
     * Extract UserPrincipal or throw.
     */
    public static UserPrincipal requireFrom(Authentication auth) {
        var up = from(auth);
        if (up == null) throw new SecurityException("Authentication required");
        return up;
    }
}
