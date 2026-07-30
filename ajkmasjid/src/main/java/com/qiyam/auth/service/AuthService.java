package com.qiyam.auth.service;

import com.qiyam.auth.dto.LoginRequest;
import com.qiyam.auth.dto.LoginResponse;
import com.qiyam.auth.dto.MosqueMembership;
import com.qiyam.shared.client.SupabaseClient;
import com.qiyam.shared.security.GoogleAuthClient;
import com.qiyam.shared.security.Role;
import com.qiyam.shared.security.RolePermissionService;
import com.qiyam.shared.security.SessionStore;
import com.qiyam.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final SupabaseClient supabaseClient;
    private final GoogleAuthClient googleAuthClient;
    private final SessionStore sessionStore;
    private final RolePermissionService rolePermissionService;

    /**
     * Fetch per-mosque memberships for the currently authenticated user.
     * Called by the frontend on page reload to rebuild in-memory state
     * without persisting sensitive permission data in localStorage.
     */
    public List<MosqueMembership> getMemberships(UserPrincipal principal) {
        var userId = principal.userId().toString();

        // SUPER_ADMIN — return empty (frontend handles unrestricted access)
        if (principal.isSuperAdmin()) {
            return List.of();
        }

        var dbMemberships = supabaseClient.getAll("mosque_committees", Map.of("user_id", "eq." + userId), Map.class);
        if (dbMemberships == null || dbMemberships.isEmpty()) {
            return List.of();
        }

        var membershipList = new ArrayList<MosqueMembership>();
        for (var m : dbMemberships) {
            var committeeRoleId = m.get("committee_role_id");
            var mosqueIdObj = m.get("mosque_id");
            if (committeeRoleId == null || !(mosqueIdObj instanceof Number mosqueNum)) continue;

            int mosqueId = mosqueNum.intValue();
            try {
                Map<String, String> roleParams = new HashMap<>();
                roleParams.put("id", "eq." + committeeRoleId.toString());
                var committeeRoles = supabaseClient.getAll("committee_roles", roleParams, Map.class);
                if (committeeRoles != null && !committeeRoles.isEmpty()) {
                    var roleName = (String) committeeRoles.get(0).get("role_name");
                    if (roleName != null) {
                        var committeeRole = Role.fromString(roleName);
                        var rolePermissions = rolePermissionService.getPermissions(committeeRole)
                                .stream()
                                .map(Enum::name)
                                .toList();
                        membershipList.add(new MosqueMembership(mosqueId, roleName, rolePermissions));
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to load permissions for committee_role_id {} at mosque {}: {}",
                        committeeRoleId, mosqueId, e.getMessage());
            }
        }

        // Fallback: user has mosqueIds in JWT but no committee role (shouldn't happen normally)
        if (membershipList.isEmpty() && principal.mosqueIds() != null) {
            var basePermissions = rolePermissionService.getPermissions(principal.role())
                    .stream()
                    .map(Enum::name)
                    .toList();
            for (var mid : principal.mosqueIds()) {
                membershipList.add(new MosqueMembership(mid, principal.role().name(), basePermissions));
            }
        }

        return membershipList;
    }

    public LoginResponse login(LoginRequest request) {
        // Step 1: Verify the Google ID token from frontend sign-in
        log.info("Login attempt with accessToken length={}", request.accessToken().length());
        var googleUser = googleAuthClient.verifyIdToken(request.accessToken());
        var email = (String) googleUser.getOrDefault("email", "");
        var fullName = (String) googleUser.getOrDefault("name", "");
        var picture = (String) googleUser.getOrDefault("picture", "");
        log.info("Google verified: email='{}', name='{}'", email, fullName);

        if (email.isBlank()) {
            log.warn("Login denied: no email returned from Google");
            throw new IllegalArgumentException("Google authentication failed: no email returned");
        }

        // Step 2: Check if the user exists in the local users table
        var dbUserOpt = supabaseClient.getOne("users", "email", email, Map.class);

        if (dbUserOpt.isEmpty()) {
            log.warn("Login denied: email '{}' not found in users table", email);
            throw new IllegalArgumentException(
                    "User not found. Please register via Qiyam mobile apps first.");
        }
        var dbUser = dbUserOpt.get();

        var userId = dbUser.get("id").toString();
        var username = (String) dbUser.getOrDefault("username", email);
        var dbFullName = (String) dbUser.getOrDefault("fullname", fullName.isBlank() ? email : fullName);
        var roleStr = (String) dbUser.getOrDefault("role", "PUBLIC_USER");

        var role = Role.fromString(roleStr);

        // If the stored role is invalid/broken (falls back to GUEST with level 999),
        // fix it by updating to PUBLIC_USER in the DB. Non-fatal if DB update fails.
        if (role.level() == 999 && !roleStr.equalsIgnoreCase("PUBLIC_USER")) {
            log.warn("User '{}' has invalid role '{}' (level=999) – auto-fixing to PUBLIC_USER", email, roleStr);
            try {
                supabaseClient.patch("users", "id", userId, Map.of("role", "PUBLIC_USER"), Map.class);
                log.info("Fixed role for user '{}' from '{}' to PUBLIC_USER", email, roleStr);
            } catch (Exception e) {
                log.error("Failed to auto-fix role for user '{}': {}", email, e.getMessage());
            }
            roleStr = "PUBLIC_USER";
            role = Role.fromString(roleStr);
        }

        // Step 3: Load committee memberships for this user from mosque_committees
        var dbMemberships = supabaseClient.getAll("mosque_committees", Map.of("user_id", "eq." + userId), Map.class);

        // Build set of mosque IDs the user belongs to
        Set<Integer> mosqueIds = null;
        if (dbMemberships != null && !dbMemberships.isEmpty()) {
            mosqueIds = new HashSet<>();
            for (var m : dbMemberships) {
                var mid = m.get("mosque_id");
                if (mid instanceof Number n) {
                    mosqueIds.add(n.intValue());
                }
            }
        }

        // For SUPER_ADMIN (level 1), mosqueIds is null → unrestricted access
        if (role.level() == 1) {
            mosqueIds = null;
        }

        // Base permissions from the user's system role (users.role)
        var basePermissions = rolePermissionService.getPermissions(role)
                .stream()
                .map(Enum::name)
                .collect(java.util.stream.Collectors.toSet());

        // ─── Build per-mosque membership list ───
        var membershipList = new ArrayList<MosqueMembership>();
        var allPermissionsUnion = new HashSet<>(basePermissions);

        if (dbMemberships != null && !dbMemberships.isEmpty()) {
            for (var m : dbMemberships) {
                var committeeRoleId = m.get("committee_role_id");
                var mosqueIdObj = m.get("mosque_id");
                if (committeeRoleId == null || !(mosqueIdObj instanceof Number mosqueNum)) continue;

                int mosqueId = mosqueNum.intValue();
                try {
                    Map<String, String> roleParams = new HashMap<>();
                    roleParams.put("id", "eq." + committeeRoleId.toString());
                    var committeeRoles = supabaseClient.getAll("committee_roles", roleParams, Map.class);
                    if (committeeRoles != null && !committeeRoles.isEmpty()) {
                        var roleName = (String) committeeRoles.get(0).get("role_name");
                        if (roleName != null) {
                            var committeeRole = Role.fromString(roleName);
                            var rolePermissions = rolePermissionService.getPermissions(committeeRole)
                                    .stream()
                                    .map(Enum::name)
                                    .toList();

                            membershipList.add(new MosqueMembership(mosqueId, roleName, rolePermissions));

                            // Also add to the global union (for SUPER_ADMIN / fallback)
                            allPermissionsUnion.addAll(rolePermissions);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to load permissions for committee_role_id {} at mosque {}: {}",
                            committeeRoleId, mosqueId, e.getMessage());
                }
            }
        }

        // If user has no memberships but has mosques, create placeholder entries
        // so the frontend can still show the mosque selector
        if (membershipList.isEmpty() && mosqueIds != null) {
            for (var mid : mosqueIds) {
                membershipList.add(new MosqueMembership(mid, role.name(), List.copyOf(basePermissions)));
            }
        }

        var permissions = List.copyOf(allPermissionsUnion);

        var isSuperAdmin = role.level() == 1;

        // Build UserPrincipal and create session
        var userPrincipal = new UserPrincipal(
                UUID.fromString(userId),
                username,
                email,
                dbFullName,
                role,
                mosqueIds
        );
        var sessionId = sessionStore.create(userPrincipal);

        log.info("User '{}' authenticated via Google login, role={}, level={}, isSuperAdmin={}, mosqueIds={}, session={}",
                email, role.name(), role.level(), isSuperAdmin, mosqueIds, sessionId);

        return new LoginResponse(
                UUID.fromString(userId),
                username,
                dbFullName,
                null,  // token no longer used (session via cookie)
                role,
                isSuperAdmin,
                mosqueIds,
                permissions,
                membershipList,
                sessionId
        );
    }
}
