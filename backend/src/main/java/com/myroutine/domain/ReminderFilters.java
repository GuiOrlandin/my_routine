package com.myroutine.domain;

import java.time.Instant;

public record ReminderFilters(
        ReminderStatus status,
        String source,
        Instant from,
        Instant to
) {
}
