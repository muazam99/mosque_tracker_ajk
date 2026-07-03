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
        if (dbUserOpt.isEmpty()) {
            log.warn("Login denied: email '{}' not registered in users table", email);
            throw new IllegalArgumentException(
                    "User not registered. Please register via Qiyam mobile apps first.");
        }

        var dbUser = dbUserOpt.get();
        var userId = dbUser.get("id").toString();
        var username = (String) dbUser.getOrDefault("username", email);
        var dbFullName = (String) dbUser.getOrDefault("fullname", fullName.isBlank() ? email : fullName);
        var roleStr = (String) dbUser.getOrDefault("role", "PUBLIC_USER");

        var role = Role.fromString(roleStr);

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
