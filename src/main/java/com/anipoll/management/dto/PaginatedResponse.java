package com.anipoll.management.dto;

import java.util.List;

public class PaginatedResponse<T> {
    public List<T> items;
    public long page;
    public long size;
    public long totalItems;
    public long totalPages;

    public PaginatedResponse() {
    }

    public PaginatedResponse(List<T> items, long page, long size, long totalItems) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.totalItems = totalItems;
        this.totalPages = size <= 0 ? 0 : (long) Math.ceil((double) totalItems / size);
    }
}
