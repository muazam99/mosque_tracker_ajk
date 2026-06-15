package com.qiyam.auth.dto;

import java.util.List;
import java.util.UUID;

public record LoginResponse(
        UUID userId,
        String username,
        String fullName,
        String token,
        List<String> roles,
        List<String> permissions) {}
