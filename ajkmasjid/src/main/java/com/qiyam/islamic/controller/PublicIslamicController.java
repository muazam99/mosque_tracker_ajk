package com.qiyam.islamic.controller;

import com.qiyam.islamic.dto.*;
import com.qiyam.islamic.service.HadithService;
import com.qiyam.islamic.service.HijriDateService;
import com.qiyam.islamic.service.IslamicEventService;
import com.qiyam.islamic.service.IslamicPrayerService;
import com.qiyam.islamic.service.QuranService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * Public, unauthenticated Islamic data for the landing page: prayer times, Hijri date, Islamic
 * calendar events, Quran, and Hadith. Nothing here touches mosque/AJK/user/finance/donation/
 * document data — see {@code com.qiyam.mosque}, {@code com.qiyam.finance}, etc. for that, all of
 * which remain behind the session-cookie auth in {@link com.qiyam.shared.config.SecurityConfig}.
 * These routes are additionally rate-limited per IP — see {@link com.qiyam.islamic.ratelimit.RateLimitInterceptor}.
 */
@RestController
@RequestMapping("/public/islamic")
@RequiredArgsConstructor
@Tag(name = "Public Islamic API", description = "Unauthenticated, read-only Islamic utilities for the public landing page (prayer times, Hijri calendar, events, Quran, Hadith)")
@ApiResponses({
        @ApiResponse(responseCode = "503", description = "An upstream Islamic data provider is unavailable — never returns fabricated data instead",
                content = @Content(schema = @Schema(implementation = PublicApiErrorResponse.class),
                        examples = @ExampleObject(value = "{\"success\": false, \"message\": \"Prayer time service temporarily unavailable\"}"))),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded for this IP",
                content = @Content(schema = @Schema(implementation = PublicApiErrorResponse.class),
                        examples = @ExampleObject(value = "{\"success\": false, \"message\": \"Rate limit exceeded. Please try again later.\"}")))
})
public class PublicIslamicController {

    private final IslamicPrayerService islamicPrayerService;
    private final HijriDateService hijriDateService;
    private final IslamicEventService islamicEventService;
    private final QuranService quranService;
    private final HadithService hadithService;

    @GetMapping("/today")
    @Operation(summary = "Today's Islamic overview",
            description = "Gregorian + Hijri date, whether today is Jumu'ah, and any Islamic events falling today. "
                    + "For prayer times, call /public/islamic/prayer-times separately with coordinates.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = TodaySummaryResponse.class)))
    public ResponseEntity<TodaySummaryResponse> today() {
        var today = LocalDate.now();
        var hijri = hijriDateService.toHijri(today);
        var isJumuah = today.getDayOfWeek() == DayOfWeek.FRIDAY;
        var todayEvents = islamicEventService.getEvents(today.getYear(), today.getMonthValue()).stream()
                .filter(e -> e.gregorianDate().equals(today.toString()))
                .toList();
        return ResponseEntity.ok(new TodaySummaryResponse(
                today.toString(), hijri, today.getDayOfWeek().name(), isJumuah, todayEvents));
    }

    @GetMapping("/prayer-times")
    @Operation(summary = "Daily prayer times for a location",
            description = "Authoritative prayer times from Aladhan (aladhan.com), a widely used prayer-time backend — "
                    + "never fabricated or approximated by this service. When 'date' is today, the response also "
                    + "includes the current/next prayer so the frontend can render its own live countdown; it is "
                    + "never recomputed per-second by this API — do not poll this endpoint for a timer.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = PrayerTimesResponse.class)))
    @ApiResponse(responseCode = "400", description = "Missing/invalid coordinates or timezone",
            content = @Content(schema = @Schema(implementation = PublicApiErrorResponse.class)))
    public ResponseEntity<PrayerTimesResponse> prayerTimes(
            @Parameter(description = "Latitude, e.g. 3.139", required = true) @RequestParam double latitude,
            @Parameter(description = "Longitude, e.g. 101.6869", required = true) @RequestParam double longitude,
            @Parameter(description = "IANA timezone, e.g. Asia/Kuala_Lumpur", required = true) @RequestParam String timezone,
            @Parameter(description = "Date to calculate for; defaults to today")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Calculation method name (e.g. JAKIM, MWL, ISNA, MAKKAH, EGYPT); defaults to JAKIM")
            @RequestParam(required = false) String calculationMethod,
            @Parameter(description = "Optional display name for the location, e.g. 'Kuala Lumpur' — echoed back only, not resolved from coordinates")
            @RequestParam(required = false) String locationName,
            @Parameter(description = "Optional display country for the location, e.g. 'Malaysia' — echoed back only")
            @RequestParam(required = false) String country) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("latitude must be between -90 and 90, longitude between -180 and 180");
        }
        var effectiveDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(islamicPrayerService.getPrayerTimes(
                latitude, longitude, effectiveDate, timezone, calculationMethod, locationName, country));
    }

    @GetMapping("/hijri")
    @Operation(summary = "Gregorian-to-Hijri date conversion",
            description = "Computed via the JDK's built-in Umm al-Qura Islamic calendar (java.time.chrono.HijrahChronology) — "
                    + "deterministic, no external service call, never hardcoded.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = HijriDateResponse.class)))
    public ResponseEntity<HijriDateResponse> hijri(
            @Parameter(description = "Gregorian date to convert; defaults to today")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        var effectiveDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(new HijriDateResponse(effectiveDate.toString(), hijriDateService.toHijri(effectiveDate)));
    }

    @GetMapping("/events")
    @Operation(summary = "Islamic calendar events",
            description = "Jumu'ah, Ramadan, Eid al-Fitr, Eid al-Adha, Ashura, Islamic New Year, Mawlid, and other recurring "
                    + "events, dynamically computed from the Hijri calendar for the requested year/month — dates are never "
                    + "hardcoded per-year. Omit both params for the list of upcoming events from today.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @io.swagger.v3.oas.annotations.media.ArraySchema(schema = @Schema(implementation = IslamicEventDto.class))))
    public ResponseEntity<List<IslamicEventDto>> events(
            @Parameter(description = "Gregorian year filter, e.g. 2026") @RequestParam(required = false) Integer year,
            @Parameter(description = "Gregorian month filter (1-12); requires 'year'") @RequestParam(required = false) Integer month) {
        if (month != null && year == null) {
            throw new IllegalArgumentException("month requires year to also be given");
        }
        if (month != null && (month < 1 || month > 12)) {
            throw new IllegalArgumentException("month must be between 1 and 12");
        }
        return ResponseEntity.ok(islamicEventService.getEvents(year, month));
    }

    @GetMapping("/quran")
    @Operation(summary = "Quran text (Arabic + translation)",
            description = "Sourced live from alquran.cloud (api.alquran.cloud) — free, no API key. Returns an entire surah "
                    + "when 'ayah' is omitted, or a single ayah when given. Text is proxied verbatim, never generated.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = QuranSurahResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid surah/ayah number or unknown surah/ayah",
            content = @Content(schema = @Schema(implementation = PublicApiErrorResponse.class)))
    public ResponseEntity<QuranSurahResponse> quran(
            @Parameter(description = "Surah number, 1-114", required = true) @RequestParam int surah,
            @Parameter(description = "Ayah number within the surah; omit to fetch the whole surah")
            @RequestParam(required = false) Integer ayah,
            @Parameter(description = "alquran.cloud translation edition identifier; defaults to en.sahih (Saheeh International)")
            @RequestParam(required = false) String edition) {
        return ResponseEntity.ok(quranService.getQuran(surah, ayah, edition));
    }

    @GetMapping("/hadith")
    @Operation(summary = "A single hadith by collection and number",
            description = "Sourced live from the fawazahmed0/hadith-api open dataset (served over the jsDelivr CDN) — "
                    + "free, no API key. Text is proxied verbatim, never generated.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = HadithResponse.class)))
    @ApiResponse(responseCode = "400", description = "Unknown collection or hadith number",
            content = @Content(schema = @Schema(implementation = PublicApiErrorResponse.class)))
    public ResponseEntity<HadithResponse> hadith(
            @Parameter(description = "Collection name, e.g. bukhari, muslim, abudawud, tirmidhi, nasai, ibnmajah", required = true)
            @RequestParam String collection,
            @Parameter(description = "Hadith number within the collection", required = true) @RequestParam int number) {
        return ResponseEntity.ok(hadithService.getHadith(collection, number));
    }
}
