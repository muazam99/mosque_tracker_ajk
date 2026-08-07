package com.qiyam.islamic.service;

import com.qiyam.islamic.dto.IslamicEventDto;
import com.qiyam.islamic.util.HijriMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Computes recurring Islamic calendar events from the Hijri calendar for whichever Gregorian
 * year/month is requested (or the upcoming ones, if neither is given). Every date is derived
 * from {@link HijriDateService} at request time — nothing here is a hardcoded annual date.
 */
@Service
@RequiredArgsConstructor
public class IslamicEventService {

    private final HijriDateService hijriDateService;

    /** (name, description, hijri month 1-12, hijri day) — the *rule*, not a date. */
    private record EventDefinition(String name, String description, int hijriMonth, int hijriDay) {}

    private static final List<EventDefinition> ANNUAL_EVENTS = List.of(
            new EventDefinition("Awal Muharram (Islamic New Year)",
                    "The first day of Muharram, marking the start of the new Hijri year", 1, 1),
            new EventDefinition("Ashura",
                    "The 10th of Muharram, commemorating Allah saving Musa (AS) and the Israelites from Pharaoh",
                    1, 10),
            new EventDefinition("Mawlid al-Nabi",
                    "Commemoration of the birth of the Prophet Muhammad (SAW), 12 Rabi' al-awwal", 3, 12),
            new EventDefinition("Start of Ramadan",
                    "The first day of the month of fasting", 9, 1),
            new EventDefinition("Nuzul Al-Quran",
                    "Commemoration of the revelation of the Quran, 17 Ramadan", 9, 17),
            new EventDefinition("Eid al-Fitr",
                    "Festival marking the end of Ramadan, 1 Shawwal", 10, 1),
            new EventDefinition("Day of Arafah",
                    "The 9th of Dhu al-Hijjah, the day before Eid al-Adha", 12, 9),
            new EventDefinition("Eid al-Adha",
                    "Festival of Sacrifice marking the culmination of Hajj, 10 Dhu al-Hijjah", 12, 10)
    );

    /**
     * @param year  optional Gregorian year filter; when given without month, returns events
     *              falling anywhere in that year
     * @param month optional Gregorian month filter (1-12); requires year
     * @return events sorted soonest-first when no filter is given, otherwise chronologically
     */
    @Cacheable(value = "islamicEvents", key = "(#year != null ? #year : 'upcoming') + '-' + (#month != null ? #month : 'all')")
    public List<IslamicEventDto> getEvents(Integer year, Integer month) {
        var today = LocalDate.now();
        var referenceYear = year != null ? year : today.getYear();
        var currentHijriYear = hijriDateService.toHijri(LocalDate.of(referenceYear, 1, 1)).year();

        var events = new ArrayList<IslamicEventDto>();
        // A Gregorian year overlaps at most two Hijri years; compute one year either side to be safe.
        for (var hijriYear = currentHijriYear - 1; hijriYear <= currentHijriYear + 1; hijriYear++) {
            for (var def : ANNUAL_EVENTS) {
                addEventIfValid(events, def, hijriYear, today);
            }
        }
        addNextJumuah(events, today);

        var filtered = events.stream()
                .filter(e -> matchesFilter(e, year, month))
                .sorted(Comparator.comparingLong(IslamicEventDto::daysUntil))
                .toList();

        // No explicit filter: show only what's still upcoming (or today), capped to the next occurrence of each.
        if (year == null) {
            return filtered.stream().filter(e -> e.daysUntil() >= 0).toList();
        }
        return filtered;
    }

    private void addEventIfValid(List<IslamicEventDto> events, EventDefinition def, int hijriYear, LocalDate today) {
        try {
            var gregorian = hijriDateService.toGregorian(hijriYear, def.hijriMonth(), def.hijriDay());
            var hijriLabel = def.hijriDay() + " " + HijriMonth.ofMonthNumber(def.hijriMonth()).displayName() + " " + hijriYear;
            events.add(new IslamicEventDto(
                    def.name(), def.description(), hijriLabel, gregorian.toString(),
                    ChronoUnit.DAYS.between(today, gregorian)));
        } catch (DateTimeException e) {
            // Outside the JDK's supported Umm al-Qura range, or an invalid day for this
            // particular Hijri year/month combination — skip rather than guess a date.
        }
    }

    private void addNextJumuah(List<IslamicEventDto> events, LocalDate today) {
        var nextFriday = today.getDayOfWeek() == DayOfWeek.FRIDAY
                ? today
                : today.with(TemporalAdjusters.next(DayOfWeek.FRIDAY));
        var hijri = hijriDateService.toHijri(nextFriday);
        var hijriLabel = hijri.day() + " " + hijri.month() + " " + hijri.year();
        events.add(new IslamicEventDto(
                "Jumu'ah", "Weekly congregational Friday prayer", hijriLabel, nextFriday.toString(),
                ChronoUnit.DAYS.between(today, nextFriday)));
    }

    private boolean matchesFilter(IslamicEventDto event, Integer year, Integer month) {
        if (year == null) return true;
        var date = LocalDate.parse(event.gregorianDate());
        if (date.getYear() != year) return false;
        return month == null || date.getMonthValue() == month;
    }
}
