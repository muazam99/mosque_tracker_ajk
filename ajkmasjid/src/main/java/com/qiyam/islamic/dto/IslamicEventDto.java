package com.qiyam.islamic.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A recurring Islamic calendar event, dynamically computed for the requested window from the Hijri calendar — dates are never hardcoded per-year.")
public record IslamicEventDto(
        @Schema(example = "Ashura") String name,
        @Schema(example = "Fasting day commemorating the day Allah saved Musa (AS) and the Israelites from Pharaoh") String description,
        @Schema(example = "10 Muharram 1448") String hijriDate,
        @Schema(example = "2026-07-26") String gregorianDate,
        @Schema(example = "42", description = "Days from today until this event; negative if already passed within the requested window") long daysUntil) {}
