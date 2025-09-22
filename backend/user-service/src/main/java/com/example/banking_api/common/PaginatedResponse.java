package com.example.banking_api.common;

import java.util.List;

public record PaginatedResponse<T>(
        List<T> items,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean last
) {
}
