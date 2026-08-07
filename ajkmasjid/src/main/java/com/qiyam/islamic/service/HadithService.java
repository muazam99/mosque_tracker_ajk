package com.qiyam.islamic.service;

import com.qiyam.islamic.dto.HadithResponse;
import com.qiyam.islamic.provider.HadithProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HadithService {

    private final HadithProvider hadithProvider;

    @Cacheable(value = "hadith", key = "#collection + '_' + #number")
    public HadithResponse getHadith(String collection, int number) {
        if (collection == null || collection.isBlank()) {
            throw new IllegalArgumentException("collection is required, e.g. bukhari, muslim, abudawud");
        }
        if (number < 1) {
            throw new IllegalArgumentException("number must be a positive hadith number");
        }
        return hadithProvider.getHadith(collection.trim(), number);
    }
}
