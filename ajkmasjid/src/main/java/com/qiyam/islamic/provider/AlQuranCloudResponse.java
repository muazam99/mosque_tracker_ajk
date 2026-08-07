package com.qiyam.islamic.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Raw deserialization targets for {@code api.alquran.cloud}'s "combined editions" endpoints
 * (verified against a live response — see git history/PR description for the exact payloads).
 * Internal to {@link QuranProvider} only; never exposed directly as an API response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record AlQuranCloudResponse(int code, String status, List<EditionBlock> data) {

    /** One block per requested edition — {@code GET /surah/{n}/editions/a,b} returns two of these. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record EditionBlock(
            int number,
            String name,
            String englishName,
            String englishNameTranslation,
            String revelationType,
            Integer numberOfAyahs,
            List<Ayah> ayahs,
            Edition edition,
            // Present only on single-ayah responses (GET /ayah/{surah}:{n}/editions/...):
            String text,
            Integer numberInSurah,
            SurahMeta surah) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Ayah(int number, String text, int numberInSurah) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Edition(String identifier, String language, String name, String type) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SurahMeta(int number, String name, String englishName, String englishNameTranslation,
                      int numberOfAyahs, String revelationType) {}
}
