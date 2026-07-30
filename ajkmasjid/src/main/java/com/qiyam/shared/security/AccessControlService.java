package com.qiyam.shared.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Centralized service for role-based access control checks.
 * Permissions are loaded dynamically from Supabase via {@link RolePermissionService}.
 */
@Slf4j
@Service
public class AccessControlService {

    private final RolePermissionService rolePermissionService;

    public AccessControlService(RolePermissionService rolePermissionService) {
        this.rolePermissionService = rolePermissionService;
    }

    /**
     * Resolves authentication from parameter or security context.
     */
    private Authentication resolveAuth(Authentication auth) {
        return auth != null ? auth : SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Checks if the authenticated user has the given permission.
     */
    public void requirePermission(Authentication auth, Permission permission) {
        var user = UserPrincipal.requireFrom(resolveAuth(auth));
        if (!rolePermissionService.hasPermission(user.role(), permission)) {
            log.warn("Access denied: user {} role {} missing permission {}",
                    user.userId(), user.role().name(), permission);
            throw new AccessDeniedException("Insufficient permissions: " + permission);
        }
    }

    /**
     * Checks if the user has permission AND mosque access.
     */
    public void requirePermissionAndMosque(Authentication auth, Permission permission, Integer mosqueId) {
        var user = UserPrincipal.requireFrom(resolveAuth(auth));
        requirePermission(auth, permission);
        if (!user.hasMosqueAccess(mosqueId)) {
            log.warn("Mosque access denied: user {} mosques {} does not include {}",
                    user.userId(), user.mosqueIds(), mosqueId);
            throw new AccessDeniedException("Access denied to this mosque's data");
        }
    }

    /**
     * Checks if the user can manage another user (hierarchical role check).
     * Uses dynamic role hierarchy levels from Supabase.
     */
    public void requireCanManageUser(Authentication auth, Role targetRole) {
        var user = UserPrincipal.requireFrom(resolveAuth(auth));
        if (!user.role().canManage(targetRole) && !user.role().name().equals(targetRole.name())) {
            log.warn("User management denied: {} (level {}) cannot manage {} (level {})",
                    user.role().name(), user.role().level(), targetRole.name(), targetRole.level());
            throw new AccessDeniedException("Cannot manage users with equal or higher role");
        }
    }

    /**
     * Checks if the user has at least the specified role level.
     */
    public void requireAtLeast(Authentication auth, int minimumLevel) {
        var user = UserPrincipal.requireFrom(resolveAuth(auth));
        if (user.role().level() > minimumLevel) {
            throw new AccessDeniedException("Requires role level <= " + minimumLevel);
        }
    }

    /**
     * Returns the user's primary mosque ID from authentication.
     * For SUPER_ADMIN, returns null (no restriction).
     * For regular users, returns the first mosque ID or null if none.
     * @deprecated Use {@link #getMosqueIds(Authentication)} for multi-mosque support.
     */
    @Deprecated
    public Integer getMosqueId(Authentication auth) {
        var user = UserPrincipal.from(resolveAuth(auth));
        if (user == null) return null;
        if (user.isSuperAdmin()) return null;
        var ids = user.mosqueIds();
        return ids != null && !ids.isEmpty() ? ids.iterator().next() : null;
    }

    /**
     * Returns all mosque IDs the user has access to.
     * SUPER_ADMIN returns null (unlimited).
     */
    public Set<Integer> getMosqueIds(Authentication auth) {
        var user = UserPrincipal.from(resolveAuth(auth));
        if (user == null) return java.util.Collections.emptySet();
        if (user.isSuperAdmin()) return null; // null = all access
        return user.mosqueIds() != null ? user.mosqueIds() : java.util.Collections.emptySet();
    }
}
