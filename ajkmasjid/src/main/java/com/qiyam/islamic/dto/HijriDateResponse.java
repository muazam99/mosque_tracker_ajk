package com.qiyam.islamic.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Gregorian-to-Hijri conversion for a single date, computed via the JDK's built-in Umm al-Qura Islamic calendar (java.time.chrono.HijrahChronology) — no external service call.")
public record HijriDateResponse(
        @Schema(example = "2026-08-07") String gregorian,
        HijriDto hijri) {}
