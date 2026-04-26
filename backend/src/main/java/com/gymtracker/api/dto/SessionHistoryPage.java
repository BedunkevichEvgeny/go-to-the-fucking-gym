package com.gymtracker.api.dto;

import java.util.List;

public record SessionHistoryPage(
        List<SessionHistoryItem> items,
        int page,
        int size,
        long totalItems
) {
}

