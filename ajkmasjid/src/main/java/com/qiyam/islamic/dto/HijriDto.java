package com.qiyam.islamic.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record HijriDto(
        @Schema(example = "12") int day,
        @Schema(example = "Sha'ban") String month,
        @Schema(example = "1") int monthNumber,
        @Schema(example = "1448") int year) {}
