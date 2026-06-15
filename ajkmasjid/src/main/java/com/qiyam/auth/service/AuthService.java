package com.qiyam.auth.service;

import com.qiyam.auth.dto.LoginRequest;
import com.qiyam.auth.dto.LoginResponse;
import com.qiyam.shared.client.SupabaseClient;
import com.qiyam.shared.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final SupabaseClient supabaseClient;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponse login(LoginRequest request) {
        var user = supabaseClient.authenticate(request.email(), request.password());
        var userId = user.get("id").toString();
        var email = (String) user.getOrDefault("email", userId);
        var username = email;

        // Generate our own JWT token with basic role (actual roles from DB if needed)
        var role = (String) user.getOrDefault("role", "user");
        var token = jwtTokenProvider.generateToken(
                UUID.fromString(userId), username, role.toUpperCase());

        log.info("User '{}' authenticated via Supabase", email);

        return new LoginResponse(
                UUID.fromString(userId),
                username,
                (String) user.getOrDefault("fullname", email),
                token,
                List.of(role),
                List.of()
        );
    }
}
