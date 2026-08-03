package com.qiyam.shared.dto;

import java.util.List;

/**
 * Standard envelope for every paginated list endpoint: the current page of rows plus enough
 * metadata for a client to render pagination controls without doing its own arithmetic.
 */
public record PagedResponse<T>(List<T> data, long total, int page, int perPage, int totalPages) {

    public static <T> PagedResponse<T> of(List<T> data, long total, int page, int perPage) {
        var totalPages = perPage <= 0 ? 0 : (int) Math.ceil((double) total / perPage);
        return new PagedResponse<>(data, total, page, perPage, totalPages);
    }

    public static <T> PagedResponse<T> empty(int page, int perPage) {
        return new PagedResponse<>(List.of(), 0, page, perPage, 0);
    }
}
