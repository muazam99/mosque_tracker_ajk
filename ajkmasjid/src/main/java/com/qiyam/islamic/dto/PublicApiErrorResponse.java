package com.qiyam.islamic.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error shape returned by every public Islamic endpoint on failure.")
public record PublicApiErrorResponse(boolean success, String message) {
    public static PublicApiErrorResponse of(String message) {
        return new PublicApiErrorResponse(false, message);
    }
}
