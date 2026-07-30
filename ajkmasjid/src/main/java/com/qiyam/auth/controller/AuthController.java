package com.qiyam.auth.controller;

import com.qiyam.auth.dto.LoginRequest;
import com.qiyam.auth.dto.LoginResponse;
import com.qiyam.auth.dto.MosqueMembership;
import com.qiyam.auth.service.AuthService;
import com.qiyam.shared.security.SessionStore;
import com.qiyam.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login and token management")
public class AuthController {
    private final AuthService authService;
    private final SessionStore sessionStore;

    private static final String SESSION_COOKIE = "qiyam_session";

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and set session cookie")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                                HttpServletResponse response) {
        var loginResponse = authService.login(request);

        // Set httpOnly session cookie
        var cookie = new Cookie(SESSION_COOKIE, loginResponse.sessionId());
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production with HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(24 * 60 * 60); // 24 hours
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);

        // Return response without sessionId (cookie already set)
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and clear session")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        // Extract session cookie and invalidate
        if (request.getCookies() != null) {
            Arrays.stream(request.getCookies())
                    .filter(c -> SESSION_COOKIE.equals(c.getName()))
                    .findFirst()
                    .ifPresent(c -> sessionStore.remove(c.getValue()));
        }

        // Clear cookie
        var cookie = new Cookie(SESSION_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "userId", principal.userId().toString(),
                "username", principal.username(),
                "fullName", principal.fullName(),
                "role", Map.of("name", principal.role().name(), "level", principal.role().level()),
                "isSuperAdmin", principal.isSuperAdmin(),
                "mosqueIds", principal.mosqueIds()
        ));
    }

    @GetMapping("/memberships")
    @Operation(summary = "Get per-mosque committee memberships for the current user")
    public ResponseEntity<List<MosqueMembership>> getMemberships(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.getMemberships(principal));
    }
}
