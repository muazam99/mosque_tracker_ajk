package com.qiyam.islamic.service;

import com.qiyam.islamic.dto.LocationDto;
import com.qiyam.islamic.dto.NextPrayerDto;
import com.qiyam.islamic.dto.PrayerTimesDto;
import com.qiyam.islamic.dto.PrayerTimesResponse;
import com.qiyam.islamic.provider.PrayerTimeProvider;
import com.qiyam.shared.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates a prayer-times lookup: delegates the actual calculation to {@link PrayerTimeProvider},
 * then (only for "today") works out the current/next prayer so the frontend can render and run its
 * own live countdown locally — this service never computes a countdown itself.
 */
@Service
@RequiredArgsConstructor
public class IslamicPrayerService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final PrayerTimeProvider prayerTimeProvider;
    private final AppProperties appProperties;
    private final Clock clock;

    /**
     * Cache key rounds lat/long to 2 decimal places (~1.1km) so nearby callers share a cache
     * entry instead of fragmenting the cache by float noise, while staying far more precise
     * than needed for prayer-time calculation purposes.
     */
    @Cacheable(value = "prayerTimes", key =
            "T(Math).round(#latitude * 100) + '_' + T(Math).round(#longitude * 100) + '_' + #date + '_' + #timezone + '_' + #calculationMethod + '_' + #locationName + '_' + #country")
    public PrayerTimesResponse getPrayerTimes(double latitude, double longitude, LocalDate date,
                                               String timezone, String calculationMethod,
                                               String locationName, String country) {
        var effectiveMethod = calculationMethod != null && !calculationMethod.isBlank()
                ? calculationMethod : appProperties.islamic().defaultCalculationMethod();
        var prayers = prayerTimeProvider.getPrayerTimes(latitude, longitude, date, timezone, effectiveMethod);

        var location = new LocationDto(locationName, country, latitude, longitude);
        var nextPrayer = date.isEqual(LocalDate.now(clock.withZone(resolveZone(timezone))))
                ? computeNextPrayer(prayers, timezone)
                : null;

        return new PrayerTimesResponse(location, date.toString(), timezone, effectiveMethod, prayers, nextPrayer);
    }

    private record ScheduleEntry(String name, LocalTime time) {}

    private NextPrayerDto computeNextPrayer(PrayerTimesDto prayers, String timezone) {
        var now = ZonedDateTime.now(clock.withZone(resolveZone(timezone))).toLocalTime();

        // Ordered obligatory prayers only — sunrise marks the end of Fajr's window, not a prayer itself.
        List<ScheduleEntry> schedule = new ArrayList<>();
        schedule.add(new ScheduleEntry("Fajr", parseTime(prayers.fajr())));
        schedule.add(new ScheduleEntry("Dhuhr", parseTime(prayers.dhuhr())));
        schedule.add(new ScheduleEntry("Asr", parseTime(prayers.asr())));
        schedule.add(new ScheduleEntry("Maghrib", parseTime(prayers.maghrib())));
        schedule.add(new ScheduleEntry("Isha", parseTime(prayers.isha())));

        var currentIndex = -1;
        for (var i = 0; i < schedule.size(); i++) {
            var entry = schedule.get(i);
            if (entry.time() != null && !now.isBefore(entry.time())) {
                currentIndex = i;
            }
        }

        // Before Fajr: nothing has started yet today, so the next prayer is Fajr itself.
        if (currentIndex == -1) {
            return new NextPrayerDto(null, "Fajr", prayers.fajr(), timezone);
        }
        // Past Isha (last entry): no further prayer time is known without tomorrow's date.
        if (currentIndex == schedule.size() - 1) {
            return new NextPrayerDto(schedule.get(currentIndex).name(), null, null, timezone);
        }
        var current = schedule.get(currentIndex);
        var next = schedule.get(currentIndex + 1);
        return new NextPrayerDto(current.name(), next.name(), TIME_FORMAT.format(next.time()), timezone);
    }

    private LocalTime parseTime(String value) {
        return value != null && !value.isBlank() ? LocalTime.parse(value, TIME_FORMAT) : null;
    }

    private ZoneId resolveZone(String timezone) {
        try {
            return timezone != null && !timezone.isBlank() ? ZoneId.of(timezone) : ZoneId.systemDefault();
        } catch (Exception e) {
            return ZoneId.systemDefault();
        }
    }
}
