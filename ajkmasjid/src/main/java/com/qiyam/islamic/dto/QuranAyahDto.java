package com.qiyam.islamic.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record QuranAyahDto(
        @Schema(example = "1") int numberInSurah,
        @Schema(example = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ") String arabicText,
        @Schema(example = "In the name of Allah, the Entirely Merciful, the Especially Merciful.") String translation) {}
