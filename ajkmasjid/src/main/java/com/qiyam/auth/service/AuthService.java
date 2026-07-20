package com.qiyam.auth.service;

import com.qiyam.auth.dto.LoginRequest;
import com.qiyam.auth.dto.LoginResponse;
import com.qiyam.shared.client.SupabaseClient;
import com.qiyam.shared.security.GoogleAuthClient;
import com.qiyam.shared.security.JwtTokenProvider;
import com.qiyam.shared.security.Role;
import com.qiyam.shared.security.RolePermissionService;
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
    private final JwtTokenProvider jwtTokenProvider;
    private final RolePermissionService rolePermissionService;

    public LoginResponse login(LoginRequest request) {
        // Step 1: Verify the Google ID token from frontend sign-in
        var googleUser = googleAuthClient.verifyIdToken(request.accessToken());
        var email = (String) googleUser.getOrDefault("email", "");
        var fullName = (String) googleUser.getOrDefault("name", "");
        var picture = (String) googleUser.getOrDefault("picture", "");

        if (email.isBlank()) {
            log.warn("Login denied: no email returned from Google");
            throw new IllegalArgumentException("Google authentication failed: no email returned");
        }

        // Step 2: Check if the user exists in the local users table
        var dbUserOpt = supabaseClient.getOne("users", "email", email, Map.class);

        final Map<String, Object> dbUser;
        if (dbUserOpt.isEmpty()) {
            // Auto-register new Google-authenticated user with PUBLIC_USER role
            log.info("Email '{}' not found in users table – auto-registering as PUBLIC_USER", email);
            var newUser = new HashMap<String, Object>();
            newUser.put("email", email);
            newUser.put("username", email);
            newUser.put("fullname", fullName.isBlank() ? email : fullName);
            newUser.put("role", "PUBLIC_USER");
            newUser.put("status", "active");
            if (picture != null && !picture.isBlank()) {
                newUser.put("image_path", picture);
            }
            dbUser = supabaseClient.post("users", newUser, Map.class);
            if (dbUser == null || dbUser.isEmpty()) {
                log.error("Failed to auto-register user '{}'", email);
                throw new IllegalArgumentException(
                        "Failed to create user account. Please try again.");
            }
            log.info("Auto-registered user '{}' with id={}", email, dbUser.get("id"));
        } else {
            dbUser = dbUserOpt.get();
        }

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

        var permissions = rolePermissionService.getPermissions(role)
                .stream()
                .map(Enum::name)
                .toList();

        Integer mosqueId = null;
        var mosqueIdObj = dbUser.get("mosque_id");
        if (mosqueIdObj instanceof Number n) {
            mosqueId = n.intValue();
        }

        var token = jwtTokenProvider.generateToken(
                UUID.fromString(userId), username, role.name(), mosqueId);

        log.info("User '{}' authenticated via Google login, role={}, level={}, mosqueId={}",
                email, role.name(), role.level(), mosqueId);

        return new LoginResponse(
                UUID.fromString(userId),
                username,
                dbFullName,
                token,
                role,
                mosqueId,
                permissions
        );
    }
}
