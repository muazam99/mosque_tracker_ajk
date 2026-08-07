package com.qiyam.islamic.provider;

import com.qiyam.islamic.dto.PrayerTimesDto;

import java.time.LocalDate;

/**
 * Abstraction over whichever external source (or local calculation library) actually computes
 * prayer times, so the provider can be swapped without touching the controller/service layer.
 */
public interface PrayerTimeProvider {

    /**
     * @param calculationMethod provider-specific method name (e.g. "JAKIM", "MWL", "ISNA");
     *                          unrecognized values fall back to the provider's default
     * @throws com.qiyam.islamic.exception.ExternalServiceUnavailableException if the times
     *         cannot be obtained — callers must never substitute fabricated times
     */
    PrayerTimesDto getPrayerTimes(double latitude, double longitude, LocalDate date,
                                   String timezone, String calculationMethod);
}
