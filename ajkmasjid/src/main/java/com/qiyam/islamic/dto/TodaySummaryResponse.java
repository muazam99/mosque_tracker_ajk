package com.qiyam.islamic.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "A lightweight daily overview: today's Gregorian/Hijri date, whether it's Jumu'ah, and any Islamic events that fall today. For prayer times, call /public/islamic/prayer-times separately with coordinates.")
public record TodaySummaryResponse(
        @Schema(example = "2026-08-07") String gregorian,
        HijriDto hijri,
        @Schema(example = "FRIDAY") String dayOfWeek,
        @Schema(example = "true") boolean isJumuah,
        List<IslamicEventDto> todayEvents) {}
