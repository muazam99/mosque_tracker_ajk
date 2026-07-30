package com.qiyam.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

/**
 * Extracts the session cookie ("qiyam_session") on every request,
 * looks up the UserPrincipal in {@link SessionStore}, and sets
 * the Spring Security context.
 *
 * Replaces the JWT-based {@link JwtAuthenticationFilter}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionCookieFilter extends OncePerRequestFilter {

    private static final String COOKIE_NAME = "qiyam_session";

    private final SessionStore sessionStore;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        var sessionId = extractSessionId(request);
        if (sessionId != null) {
            var principalOpt = sessionStore.get(sessionId);
            if (principalOpt.isPresent()) {
                var principal = principalOpt.get();
                var auth = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities());
                auth.setDetails(principal.username());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                // Stale/expired session — clear the cookie
                var cookie = new Cookie(COOKIE_NAME, "");
                cookie.setHttpOnly(true);
                cookie.setSecure(request.isSecure());
                cookie.setPath("/");
                cookie.setMaxAge(0);
                cookie.setAttribute("SameSite", "Lax");
                response.addCookie(cookie);
            }
        }
        chain.doFilter(request, response);
    }

    private String extractSessionId(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
