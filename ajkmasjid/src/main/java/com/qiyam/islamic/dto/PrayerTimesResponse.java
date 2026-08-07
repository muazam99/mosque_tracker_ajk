package com.qiyam.islamic.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authoritative daily prayer times for a location, date, and calculation method.")
public record PrayerTimesResponse(
        LocationDto location,
        @Schema(example = "2026-08-07") String date,
        @Schema(example = "Asia/Kuala_Lumpur") String timezone,
        @Schema(example = "JAKIM", description = "Calculation method used, e.g. JAKIM (Jabatan Kemajuan Islam Malaysia)") String method,
        PrayerTimesDto prayers,
        @Schema(description = "Null unless 'date' is today") NextPrayerDto nextPrayer) {}
