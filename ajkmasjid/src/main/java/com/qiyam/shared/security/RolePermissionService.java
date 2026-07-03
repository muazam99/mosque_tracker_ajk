package com.qiyam.shared.security;

import com.qiyam.shared.client.SupabaseClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Dynamically loads roles and their permissions from Supabase:
 * - committee_roles table: role_name, hierarchy_level
 * - role_permissions table: committee_role_id (FK), permission_code
 *
 * Caches data in memory and refreshes periodically.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private final SupabaseClient supabaseClient;

    /** role name -> Role */
    private final Map<String, Role> roleCache = new ConcurrentHashMap<>();

    /** role name -> set of permission names */
    private final Map<String, Set<String>> permissionCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        refresh();
    }

    /**
     * Refresh caches from Supabase every 5 minutes.
     */
    @Scheduled(fixedDelay = 300_000)
    public void refresh() {
        try {
            // Load roles from committee_roles table
            var committeeRoles = supabaseClient.getAll("committee_roles", Map.of(), Map.class);
            if (committeeRoles != null) {
                for (var r : committeeRoles) {
                    var roleName = (String) r.get("role_name");
                    var level = r.get("hierarchy_level") instanceof Number n ? n.intValue() : 999;
                    if (roleName != null) {
                        Role.from(roleName, level);
                    }
                }
            }

            // Load all role_permissions with their committee_role_id references
            var perms = supabaseClient.getAll("role_permissions", Map.of(), Map.class);
            permissionCache.clear();
            if (perms != null) {
                // Build a lookup from committee_role_id -> role_name
                Map<Object, String> roleIdToName = new HashMap<>();
                if (committeeRoles != null) {
                    for (var r : committeeRoles) {
                        var id = r.get("id");
                        var name = (String) r.get("role_name");
                        if (id != null && name != null) {
                            roleIdToName.put(id, name.toUpperCase());
                        }
                    }
                }

                for (var p : perms) {
                    var committeeRoleId = p.get("committee_role_id");
                    var permCode = (String) p.get("permission_code");
                    var roleName = roleIdToName.get(committeeRoleId);
                    if (roleName != null && permCode != null) {
                        permissionCache.computeIfAbsent(roleName, k -> ConcurrentHashMap.newKeySet()).add(permCode);
                    }
                }
            }

            log.info("Loaded {} roles and {} role-permission mappings from Supabase",
                    roleCache.size(), permissionCache.size());
        } catch (Exception e) {
            log.error("Failed to load roles/permissions from Supabase: {}", e.getMessage());
        }
    }

    /**
     * Get effective permissions for a role as a set of Permission enums.
     */
    public Set<Permission> getPermissions(Role role) {
        var permNames = permissionCache.getOrDefault(role.name(), Set.of());
        return permNames.stream()
                .map(this::safeParsePermission)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Check if a role has a specific permission.
     */
    public boolean hasPermission(Role role, Permission permission) {
        var permNames = permissionCache.get(role.name());
        return permNames != null && permNames.contains(permission.name());
    }

    /**
     * Check if a role has at least one of the given permissions.
     */
    public boolean hasAnyPermission(Role role, Permission... permissions) {
        var permNames = permissionCache.get(role.name());
        if (permNames == null) return false;
        for (var p : permissions) {
            if (permNames.contains(p.name())) return true;
        }
        return false;
    }

    /**
     * Returns the hierarchy level of a role, or 999 if unknown.
     */
    public int getRoleLevel(String roleName) {
        var role = Role.fromString(roleName);
        return role != Role.GUEST ? role.level() : 999;
    }

    private Permission safeParsePermission(String name) {
        try {
            return Permission.valueOf(name);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown permission '{}' in role_permissions table", name);
            return null;
        }
    }
}
