package com.qiyam.islamic.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The location a prayer-time calculation was made for. name/country are only populated when the caller supplies them — this API does not reverse-geocode coordinates.")
public record LocationDto(
        @Schema(example = "Kuala Lumpur") String name,
        @Schema(example = "Malaysia") String country,
        @Schema(example = "3.139") double latitude,
        @Schema(example = "101.6869") double longitude) {}
