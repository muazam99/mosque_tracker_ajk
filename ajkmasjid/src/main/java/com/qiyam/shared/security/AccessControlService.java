package com.qiyam.shared.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

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

    /**
     * Checks the given permission, then resolves which mosque IDs a list/read query should
     * be scoped to. This is the central guard against cross-mosque data leakage on endpoints
     * that accept an optional {@code mosqueId} filter.
     *
     * <ul>
     *   <li>SUPER_ADMIN with no requested mosque → {@code null} (unrestricted, no filter applied)</li>
     *   <li>Any user requesting a specific mosque → validated against their access, then that
     *       single mosque as a singleton set (throws {@link AccessDeniedException} if not permitted)</li>
     *   <li>Regular user with no requested mosque → their own mosque memberships (never "everything"),
     *       which may be an empty set if they belong to none</li>
     * </ul>
     */
    public Set<Integer> resolveMosqueScope(Authentication auth, Permission permission, Integer requestedMosqueId) {
        requirePermission(auth, permission);
        var user = UserPrincipal.requireFrom(resolveAuth(auth));
        if (requestedMosqueId != null) {
            if (!user.hasMosqueAccess(requestedMosqueId)) {
                log.warn("Mosque access denied: user {} mosques {} does not include {}",
                        user.userId(), user.mosqueIds(), requestedMosqueId);
                throw new AccessDeniedException("Access denied to this mosque's data");
            }
            return Set.of(requestedMosqueId);
        }
        if (user.isSuperAdmin()) return null;
        return user.mosqueIds() != null ? user.mosqueIds() : Collections.emptySet();
    }

    /**
     * Verifies the caller may access a single already-fetched row, given its raw {@code mosque_id}
     * value (typically a {@link Number} from a Supabase/PostgREST response map). Call this after
     * a get-by-id lookup to prevent cross-mosque IDOR reads on resources that don't accept a
     * {@code mosqueId} query filter.
     */
    public void requireRowMosqueAccess(Authentication auth, Object mosqueIdValue) {
        var user = UserPrincipal.requireFrom(resolveAuth(auth));
        var mosqueId = mosqueIdValue instanceof Number n ? n.intValue() : null;
        if (!user.hasMosqueAccess(mosqueId)) {
            log.warn("Row access denied: user {} mosques {} does not include {}",
                    user.userId(), user.mosqueIds(), mosqueId);
            throw new AccessDeniedException("Access denied to this mosque's data");
        }
    }
}
