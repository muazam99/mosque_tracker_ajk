package com.qiyam.islamic.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The six daily prayer times plus sunrise, as HH:mm in the requested timezone.")
public record PrayerTimesDto(
        @Schema(example = "05:51") String fajr,
        @Schema(example = "07:12") String sunrise,
        @Schema(example = "13:19") String dhuhr,
        @Schema(example = "16:39") String asr,
        @Schema(example = "19:26") String maghrib,
        @Schema(example = "20:38") String isha) {}
