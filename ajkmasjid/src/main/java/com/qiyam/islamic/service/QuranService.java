package com.qiyam.islamic.service;

import com.qiyam.islamic.dto.QuranSurahResponse;
import com.qiyam.islamic.provider.QuranProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuranService {

    private static final String DEFAULT_EDITION = "en.sahih";

    private final QuranProvider quranProvider;

    @Cacheable(value = "quran", key = "#surah + '_' + #ayah + '_' + #edition")
    public QuranSurahResponse getQuran(int surah, Integer ayah, String edition) {
        if (surah < 1 || surah > 114) {
            throw new IllegalArgumentException("surah must be between 1 and 114");
        }
        if (ayah != null && ayah < 1) {
            throw new IllegalArgumentException("ayah must be a positive number");
        }
        var effectiveEdition = edition != null && !edition.isBlank() ? edition : DEFAULT_EDITION;
        return ayah != null
                ? quranProvider.getAyah(surah, ayah, effectiveEdition)
                : quranProvider.getSurah(surah, effectiveEdition);
    }
}
