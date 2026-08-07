package com.qiyam.islamic.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enough information for the frontend to render and locally tick down a live countdown
 * ("Prayer in 01:24:36") without polling the backend. Only populated when the requested
 * date is today, since "current/next prayer" is meaningless for a past or future date.
 */
@Schema(description = "Current/next prayer relative to the server clock, only present when 'date' is today. The frontend computes and updates the countdown locally from nextPrayerTime — never poll this endpoint for a live timer.")
public record NextPrayerDto(
        @Schema(example = "Dhuhr", description = "The prayer whose window is currently active") String currentPrayer,
        @Schema(example = "Asr") String nextPrayer,
        @Schema(example = "16:39", description = "HH:mm in 'timezone' — combine with the response date to get a full instant") String nextPrayerTime,
        @Schema(example = "Asia/Kuala_Lumpur") String timezone) {}
