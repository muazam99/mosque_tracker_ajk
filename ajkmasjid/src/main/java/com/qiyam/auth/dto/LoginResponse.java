package com.qiyam.auth.dto;

import com.qiyam.shared.security.Role;

import java.util.List;
import java.util.UUID;

public record LoginResponse(
        UUID userId,
        String username,
        String fullName,
        String token,
        Role role,
        Integer mosqueId,
        List<String> permissions) {}
