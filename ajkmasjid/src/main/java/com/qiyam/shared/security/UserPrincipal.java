package com.qiyam.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Custom principal carrying user identity, role, and mosque affiliation.
 */
public record UserPrincipal(
        UUID userId,
        String username,
        String email,
        String fullName,
        Role role,
        Integer mosqueId
) {
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
     */
    public boolean hasMosqueAccess(Integer targetMosqueId) {
        if (role.level() == 1) return true; // SUPER_ADMIN level
        if (targetMosqueId == null) return true;
        return mosqueId != null && mosqueId.equals(targetMosqueId);
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
