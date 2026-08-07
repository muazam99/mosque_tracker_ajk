package com.qiyam.islamic.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Raw deserialization target for {@code GET api.aladhan.com/v1/timings/{date}} — internal to this provider only. */
@JsonIgnoreProperties(ignoreUnknown = true)
record AladhanTimingsResponse(int code, String status, Data data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Data(Timings timings) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Timings(
            @JsonProperty("Fajr") String fajr,
            @JsonProperty("Sunrise") String sunrise,
            @JsonProperty("Dhuhr") String dhuhr,
            @JsonProperty("Asr") String asr,
            @JsonProperty("Maghrib") String maghrib,
            @JsonProperty("Isha") String isha) {}
}
