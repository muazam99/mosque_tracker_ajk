package com.qiyam.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Login request using Google OAuth.
 * Frontend sends the access_token from Supabase Google sign-in.
 */
public record LoginRequest(
        @NotBlank String accessToken) {}
