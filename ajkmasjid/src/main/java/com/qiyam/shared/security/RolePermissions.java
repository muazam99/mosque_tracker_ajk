package com.qiyam.shared.security;

import java.util.Set;

/**
 * Role permissions are now loaded dynamically from Supabase
 * via {@link RolePermissionService}.
 *
 * @deprecated Use {@link RolePermissionService#getPermissions(Role)} instead.
 */
@Deprecated(forRemoval = true)
public final class RolePermissions {
    private RolePermissions() {}

    @Deprecated
    public static Set<Permission> getPermissions(Role role) {
        return Set.of();
    }

    @Deprecated
    public static boolean hasPermission(Role role, Permission permission) {
        return false;
    }
}
