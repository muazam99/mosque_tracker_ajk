package com.qiyam.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        var token = extractToken(request);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            var claims = jwtTokenProvider.parseToken(token);
            var userId = UUID.fromString(claims.getSubject());
            var username = claims.get("username", String.class);
            var roleStr = claims.get("role", String.class);
            var email = claims.get("email", String.class);
            var fullName = claims.get("fullName", String.class);
            // Parse multi-mosque support: prefer "mosqueIds" array, fall back to single "mosqueId"
            Set<Integer> mosqueIds = null;
            var mosqueIdsObj = claims.get("mosqueIds");
            if (mosqueIdsObj instanceof List<?> list) {
                mosqueIds = list.stream()
                        .filter(o -> o instanceof Number)
                        .map(o -> ((Number) o).intValue())
                        .collect(Collectors.toSet());
            } else {
                // Fallback: single mosqueId claim for backward compatibility
                var singleMosqueId = claims.get("mosqueId", Integer.class);
                if (singleMosqueId != null) {
                    mosqueIds = Set.of(singleMosqueId);
                }
            }

            var role = Role.fromString(roleStr);
            var userPrincipal = new UserPrincipal(
                    userId,
                    username,
                    email != null ? email : username,
                    fullName != null ? fullName : username,
                    role,
                    mosqueIds
            );

            var auth = new UsernamePasswordAuthenticationToken(
                    userPrincipal,
                    null,
                    userPrincipal.getAuthorities());
            auth.setDetails(username);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        var bearer = request.getHeader(HEADER);
        if (StringUtils.hasText(bearer) && bearer.startsWith(PREFIX))
            return bearer.substring(PREFIX.length());
        return null;
    }
}
