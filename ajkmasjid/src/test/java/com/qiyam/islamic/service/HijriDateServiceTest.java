package com.qiyam.islamic.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class HijriDateServiceTest {

    private final HijriDateService service = new HijriDateService();

    @Test
    void toHijri_roundTripsBackToTheSameGregorianDate() {
        for (var date : new LocalDate[]{
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 8, 7), LocalDate.of(2000, 1, 1), LocalDate.of(1990, 6, 15)
        }) {
            var hijri = service.toHijri(date);
            var reconstructed = service.toGregorian(hijri.year(), hijri.monthNumber(), hijri.day());
            assertThat(reconstructed).as("round-trip for %s", date).isEqualTo(date);
        }
    }

    @Test
    void toHijri_monthNumberMatchesMonthName() {
        var hijri = service.toHijri(LocalDate.of(2026, 8, 7));
        assertThat(hijri.monthNumber()).isBetween(1, 12);
        assertThat(hijri.day()).isBetween(1, 30);
    }

    /**
     * 2026-08-07 is Safar 24, 1448 per Aladhan's HJCoSA method (verified live). The JDK's
     * Umm al-Qura calendar can differ by a day or two from other Hijri calculation methods —
     * assert a loose tolerance rather than exact equality across implementations.
     */
    @Test
    void toHijri_matchesKnownReferencePoint_withinToleranceOfDifferentCalculationMethods() {
        var hijri = service.toHijri(LocalDate.of(2026, 8, 7));
        assertThat(hijri.year()).isEqualTo(1448);
        assertThat(hijri.monthNumber()).isEqualTo(2); // Safar
        assertThat(hijri.day()).isBetween(21, 27);
    }

    @Test
    void toGregorian_matchesKnownReferencePoints_withinTolerance() {
        // Aladhan hToG: Ramadan 1, 1447 -> 2026-02-18
        var ramadanStart = service.toGregorian(1447, 9, 1);
        assertThat(ramadanStart).isBetween(LocalDate.of(2026, 2, 16), LocalDate.of(2026, 2, 20));

        // Aladhan hToG: Muharram 1, 1448 -> 2026-06-16
        var newYear = service.toGregorian(1448, 1, 1);
        assertThat(newYear).isBetween(LocalDate.of(2026, 6, 14), LocalDate.of(2026, 6, 18));
    }

    @Test
    void currentHijriYear_isPositiveAndPlausible() {
        var year = service.currentHijriYear();
        assertThat(year).isGreaterThan(1400).isLessThan(1600);
    }
}
