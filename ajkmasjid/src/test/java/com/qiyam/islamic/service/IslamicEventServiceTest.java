package com.qiyam.islamic.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class IslamicEventServiceTest {

    private final HijriDateService hijriDateService = new HijriDateService();
    private final IslamicEventService service = new IslamicEventService(hijriDateService);

    @Test
    void getEvents_withNoFilter_returnsOnlyUpcomingEvents() {
        var events = service.getEvents(null, null);

        assertThat(events).isNotEmpty();
        assertThat(events).allSatisfy(e -> assertThat(e.daysUntil()).isGreaterThanOrEqualTo(0));
    }

    @Test
    void getEvents_alwaysIncludesAnUpcomingJumuahWithinTheNextWeek() {
        var events = service.getEvents(null, null);

        assertThat(events).anySatisfy(e -> {
            assertThat(e.name()).isEqualTo("Jumu'ah");
            assertThat(e.daysUntil()).isBetween(0L, 6L);
        });
    }

    @Test
    void getEvents_withYearFilter_onlyReturnsEventsInThatGregorianYear() {
        var events = service.getEvents(2026, null);

        assertThat(events).isNotEmpty();
        assertThat(events).allSatisfy(e -> assertThat(LocalDate.parse(e.gregorianDate()).getYear()).isEqualTo(2026));
    }

    @Test
    void getEvents_withYearAndMonthFilter_onlyReturnsEventsInThatMonth() {
        // Ramadan 1447 starts ~2026-02-18 per Aladhan (verified live) — filtering to Feb 2026
        // should surface "Start of Ramadan" specifically.
        var events = service.getEvents(2026, 2);

        assertThat(events).allSatisfy(e -> {
            var date = LocalDate.parse(e.gregorianDate());
            assertThat(date.getYear()).isEqualTo(2026);
            assertThat(date.getMonthValue()).isEqualTo(2);
        });
        assertThat(events).anyMatch(e -> e.name().equals("Start of Ramadan"));
    }

    @Test
    void getEvents_eventDescriptionsAreNeverBlank() {
        var events = service.getEvents(2026, null);

        assertThat(events).allSatisfy(e -> {
            assertThat(e.name()).isNotBlank();
            assertThat(e.description()).isNotBlank();
            assertThat(e.hijriDate()).isNotBlank();
        });
    }
}
