package com.qiyam.islamic.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "A surah (or a single ayah within it, when 'ayah' was given), sourced live from alquran.cloud — text is proxied verbatim, never generated.")
public record QuranSurahResponse(
        @Schema(example = "1") int number,
        @Schema(example = "الفاتحة") String name,
        @Schema(example = "Al-Faatiha") String englishName,
        @Schema(example = "The Opening") String englishNameTranslation,
        @Schema(example = "Meccan") String revelationType,
        @Schema(example = "en.sahih", description = "Translation edition identifier used") String translationEdition,
        List<QuranAyahDto> ayahs) {}
