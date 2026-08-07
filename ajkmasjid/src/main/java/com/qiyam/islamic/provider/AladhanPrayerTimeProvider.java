package com.qiyam.islamic.provider;

import com.qiyam.islamic.dto.PrayerTimesDto;
import com.qiyam.islamic.exception.ExternalServiceUnavailableException;
import com.qiyam.shared.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Fetches prayer times from Aladhan (api.aladhan.com) — free, no API key required. Aladhan
 * covers all the named calculation methods this API exposes (JAKIM included) and is one of
 * the most widely used prayer-time backends, so this is the "reliable external provider" the
 * public API is built around.
 */
@Slf4j
@Component
public class AladhanPrayerTimeProvider implements PrayerTimeProvider {

    private static final DateTimeFormatter ALADHAN_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public AladhanPrayerTimeProvider(RestTemplate restTemplate, AppProperties appProperties) {
        this.restTemplate = restTemplate;
        this.baseUrl = appProperties.islamic().aladhanBaseUrl();
    }

    @Override
    public PrayerTimesDto getPrayerTimes(double latitude, double longitude, LocalDate date,
                                          String timezone, String calculationMethod) {
        var method = AladhanCalculationMethod.fromNameOrDefault(calculationMethod);
        var uriBuilder = UriComponentsBuilder.fromUriString(baseUrl + "/timings/" + ALADHAN_DATE_FORMAT.format(date))
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("method", method.aladhanId());
        if (timezone != null && !timezone.isBlank()) {
            uriBuilder.queryParam("timezonestring", timezone);
        }
        var uri = uriBuilder.build().toUri();

        AladhanTimingsResponse response;
        try {
            response = restTemplate.getForObject(uri, AladhanTimingsResponse.class);
        } catch (RestClientException e) {
            log.error("Aladhan request failed for {}: {}", uri, e.getMessage());
            throw new ExternalServiceUnavailableException("Prayer time service temporarily unavailable", e);
        }

        if (response == null || response.data() == null || response.data().timings() == null) {
            log.error("Aladhan returned an unexpected/empty response for {}", uri);
            throw new ExternalServiceUnavailableException("Prayer time service temporarily unavailable");
        }

        var t = response.data().timings();
        return new PrayerTimesDto(
                stripTimezoneSuffix(t.fajr()),
                stripTimezoneSuffix(t.sunrise()),
                stripTimezoneSuffix(t.dhuhr()),
                stripTimezoneSuffix(t.asr()),
                stripTimezoneSuffix(t.maghrib()),
                stripTimezoneSuffix(t.isha()));
    }

    /** Aladhan sometimes appends a UTC-offset suffix like "05:51 (+08)" — keep just "HH:mm". */
    private String stripTimezoneSuffix(String time) {
        if (time == null) return null;
        var spaceIdx = time.indexOf(' ');
        return spaceIdx > 0 ? time.substring(0, spaceIdx) : time;
    }
}
