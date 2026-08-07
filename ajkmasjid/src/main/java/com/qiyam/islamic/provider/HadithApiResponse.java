package com.qiyam.islamic.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Raw deserialization target for the fawazahmed0/hadith-api dataset served over jsDelivr
 * ({@code GET /editions/{edition}/{hadithnumber}.min.json}) — verified against a live response.
 * Internal to {@link HadithProvider} only.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record HadithApiResponse(Metadata metadata, List<RawHadith> hadiths) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Metadata(String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RawHadith(int hadithnumber, Integer arabicnumber, String text, List<String> grades, Reference reference) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Reference(int book, int hadith) {}
    }
}
