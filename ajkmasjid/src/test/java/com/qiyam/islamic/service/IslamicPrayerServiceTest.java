package com.qiyam.islamic.service;

import com.qiyam.islamic.dto.PrayerTimesDto;
import com.qiyam.islamic.provider.PrayerTimeProvider;
import com.qiyam.shared.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IslamicPrayerServiceTest {

    private static final String ZONE = "Asia/Kuala_Lumpur";
    private static final PrayerTimesDto SAMPLE_TIMES =
            new PrayerTimesDto("05:51", "07:12", "13:19", "16:39", "19:26", "20:38");

    @Mock
    private PrayerTimeProvider prayerTimeProvider;

    private AppProperties appProperties;

    @BeforeEach
    void setUp() {
        var islamic = new AppProperties.Islamic("https://api.aladhan.com/v1", "JAKIM",
                "https://api.alquran.cloud/v1", "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1");
        appProperties = new AppProperties(null, null, null, null, null, null, islamic, null);
    }

    private IslamicPrayerService serviceAt(String isoInstant) {
        var clock = Clock.fixed(Instant.parse(isoInstant), ZoneId.of(ZONE));
        return new IslamicPrayerService(prayerTimeProvider, appProperties, clock);
    }

    @Test
    void getPrayerTimes_beforeFajr_currentIsNullAndNextIsFajr() {
        when(prayerTimeProvider.getPrayerTimes(any(Double.class), any(Double.class), any(), any(), any()))
                .thenReturn(SAMPLE_TIMES);
        // 2026-08-07 05:00 in Asia/Kuala_Lumpur (UTC+8) -> 2026-08-06T21:00:00Z
        var service = serviceAt("2026-08-06T21:00:00Z");

        var response = service.getPrayerTimes(3.139, 101.6869, LocalDate.of(2026, 8, 7), ZONE, "JAKIM", "KL", "Malaysia");

        assertThat(response.nextPrayer()).isNotNull();
        assertThat(response.nextPrayer().currentPrayer()).isNull();
        assertThat(response.nextPrayer().nextPrayer()).isEqualTo("Fajr");
        assertThat(response.nextPrayer().nextPrayerTime()).isEqualTo("05:51");
    }

    @Test
    void getPrayerTimes_betweenDhuhrAndAsr_currentIsDhuhrNextIsAsr() {
        when(prayerTimeProvider.getPrayerTimes(any(Double.class), any(Double.class), any(), any(), any()))
                .thenReturn(SAMPLE_TIMES);
        // 2026-08-07 14:00 local (UTC+8) -> 2026-08-07T06:00:00Z
        var service = serviceAt("2026-08-07T06:00:00Z");

        var response = service.getPrayerTimes(3.139, 101.6869, LocalDate.of(2026, 8, 7), ZONE, "JAKIM", "KL", "Malaysia");

        assertThat(response.nextPrayer().currentPrayer()).isEqualTo("Dhuhr");
        assertThat(response.nextPrayer().nextPrayer()).isEqualTo("Asr");
        assertThat(response.nextPrayer().nextPrayerTime()).isEqualTo("16:39");
    }

    @Test
    void getPrayerTimes_afterIsha_currentIsIshaAndNextIsUnknown() {
        when(prayerTimeProvider.getPrayerTimes(any(Double.class), any(Double.class), any(), any(), any()))
                .thenReturn(SAMPLE_TIMES);
        // 2026-08-07 23:00 local (UTC+8) -> 2026-08-07T15:00:00Z
        var service = serviceAt("2026-08-07T15:00:00Z");

        var response = service.getPrayerTimes(3.139, 101.6869, LocalDate.of(2026, 8, 7), ZONE, "JAKIM", "KL", "Malaysia");

        assertThat(response.nextPrayer().currentPrayer()).isEqualTo("Isha");
        assertThat(response.nextPrayer().nextPrayer()).isNull();
        assertThat(response.nextPrayer().nextPrayerTime()).isNull();
    }

    @Test
    void getPrayerTimes_forNonTodayDate_omitsNextPrayerEntirely() {
        when(prayerTimeProvider.getPrayerTimes(any(Double.class), any(Double.class), any(), any(), any()))
                .thenReturn(SAMPLE_TIMES);
        var service = serviceAt("2026-08-06T21:00:00Z"); // "today" is 2026-08-07 in this zone

        var response = service.getPrayerTimes(3.139, 101.6869, LocalDate.of(2026, 12, 25), ZONE, "JAKIM", "KL", "Malaysia");

        assertThat(response.nextPrayer()).isNull();
    }

    @Test
    void getPrayerTimes_locationEchoesBackWhatWasSupplied() {
        when(prayerTimeProvider.getPrayerTimes(any(Double.class), any(Double.class), any(), any(), any()))
                .thenReturn(SAMPLE_TIMES);
        var service = serviceAt("2026-08-06T21:00:00Z");

        var response = service.getPrayerTimes(3.139, 101.6869, LocalDate.of(2026, 8, 7), ZONE, null, "Kuala Lumpur", "Malaysia");

        assertThat(response.location().name()).isEqualTo("Kuala Lumpur");
        assertThat(response.location().country()).isEqualTo("Malaysia");
        assertThat(response.method()).isEqualTo("JAKIM"); // falls back to app default
    }
}
