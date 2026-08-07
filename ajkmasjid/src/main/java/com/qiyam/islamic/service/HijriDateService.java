package com.qiyam.islamic.service;

import com.qiyam.islamic.dto.HijriDto;
import com.qiyam.islamic.util.HijriMonth;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.chrono.HijrahDate;
import java.time.temporal.ChronoField;

/**
 * Gregorian ⇄ Hijri conversion using the JDK's built-in {@link HijrahDate} (the Umm al-Qura
 * calendar, {@code java.time.chrono.HijrahChronology}). This is a deterministic, well-defined
 * calendrical calculation shipped with the JVM itself — no external service call, and nothing
 * about it is fabricated or approximated by this code. As with any arithmetic Hijri calendar,
 * it may differ by a day from a given country's local moon-sighting announcement.
 */
@Service
public class HijriDateService {

    @Cacheable(value = "hijriDates", key = "#gregorianDate")
    public HijriDto toHijri(LocalDate gregorianDate) {
        var hijrahDate = HijrahDate.from(gregorianDate);
        var day = hijrahDate.get(ChronoField.DAY_OF_MONTH);
        var month = hijrahDate.get(ChronoField.MONTH_OF_YEAR);
        var year = hijrahDate.get(ChronoField.YEAR_OF_ERA);
        return new HijriDto(day, HijriMonth.ofMonthNumber(month).displayName(), month, year);
    }

    /** Converts a specific Hijri (year, 1-indexed month, day) back to its Gregorian date. */
    public LocalDate toGregorian(int hijriYear, int hijriMonth, int hijriDay) {
        return LocalDate.ofEpochDay(HijrahDate.of(hijriYear, hijriMonth, hijriDay).toEpochDay());
    }

    public int currentHijriYear() {
        return HijrahDate.now().get(ChronoField.YEAR_OF_ERA);
    }
}
