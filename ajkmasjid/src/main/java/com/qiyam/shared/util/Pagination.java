package com.qiyam.shared.util;

/**
 * Resolves the (limit, offset, page) trio that every list controller accepts into a single
 * consistent set of effective values, so callers can use {@code page} (1-indexed) *or* the
 * older {@code limit}/{@code offset} pair interchangeably.
 */
public final class Pagination {

    private Pagination() {}

    /** {@code per_page} wins over {@code limit} when both are given (mirrors the existing alias). */
    public static int resolveLimit(Integer perPage, int limit) {
        return perPage != null && perPage > 0 ? perPage : limit;
    }

    /** {@code page} (1-indexed), when given, takes precedence over an explicit {@code offset}. */
    public static int resolveOffset(Integer page, int effectiveLimit, int offset) {
        return page != null && page >= 1 ? (page - 1) * effectiveLimit : offset;
    }

    /** The page number implied by the effective offset/limit, for when the caller didn't pass one. */
    public static int resolvePage(Integer page, int effectiveOffset, int effectiveLimit) {
        if (page != null && page >= 1) return page;
        return effectiveLimit <= 0 ? 1 : (effectiveOffset / effectiveLimit) + 1;
    }
}
