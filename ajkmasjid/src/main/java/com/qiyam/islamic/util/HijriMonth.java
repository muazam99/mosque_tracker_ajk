package com.qiyam.islamic.util;

/** The twelve Hijri calendar months — a fixed reference list, not date data, so it's safe to keep as code. */
public enum HijriMonth {
    MUHARRAM("Muharram"),
    SAFAR("Safar"),
    RABI_AL_AWWAL("Rabi' al-awwal"),
    RABI_AL_THANI("Rabi' al-thani"),
    JUMADA_AL_AWWAL("Jumada al-awwal"),
    JUMADA_AL_THANI("Jumada al-thani"),
    RAJAB("Rajab"),
    SHABAN("Sha'ban"),
    RAMADAN("Ramadan"),
    SHAWWAL("Shawwal"),
    DHU_AL_QIDAH("Dhu al-Qi'dah"),
    DHU_AL_HIJJAH("Dhu al-Hijjah");

    private final String displayName;

    HijriMonth(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /** @param monthNumber 1-indexed, as returned by {@link java.time.chrono.HijrahDate} */
    public static HijriMonth ofMonthNumber(int monthNumber) {
        return values()[monthNumber - 1];
    }
}
