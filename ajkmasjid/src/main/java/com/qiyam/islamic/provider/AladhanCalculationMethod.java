package com.qiyam.islamic.provider;

import java.util.Arrays;

/**
 * Maps a human-readable calculation-method name to Aladhan's numeric method id.
 * See <a href="https://aladhan.com/calculation-methods">aladhan.com/calculation-methods</a>.
 */
public enum AladhanCalculationMethod {
    JAFARI(0),
    KARACHI(1),
    ISNA(2),
    MWL(3),
    MAKKAH(4),
    EGYPT(5),
    TEHRAN(7),
    GULF(8),
    KUWAIT(9),
    QATAR(10),
    SINGAPORE(11),
    FRANCE(12),
    TURKEY(13),
    RUSSIA(14),
    MOONSIGHTING(15),
    DUBAI(16),
    JAKIM(17),
    TUNISIA(18),
    ALGERIA(19),
    INDONESIA(20),
    MOROCCO(21),
    PORTUGAL(22),
    JORDAN(23);

    private final int aladhanId;

    AladhanCalculationMethod(int aladhanId) {
        this.aladhanId = aladhanId;
    }

    public int aladhanId() {
        return aladhanId;
    }

    /** Falls back to {@link #JAKIM} for null/unrecognized names — matches this app's regional default. */
    public static AladhanCalculationMethod fromNameOrDefault(String name) {
        if (name == null || name.isBlank()) return JAKIM;
        return Arrays.stream(values())
                .filter(m -> m.name().equalsIgnoreCase(name.trim()))
                .findFirst()
                .orElse(JAKIM);
    }
}
