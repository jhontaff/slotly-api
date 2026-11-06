package com.slotly_api.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record TimeRange(Instant start, Instant end) {

    public TimeRange {
        Objects.requireNonNull(start, "Start time cannot be null");
        Objects.requireNonNull(end, "End time cannot be null");
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Start must be before end: [%s, %s)".formatted(start, end));
        }
    }

    public static TimeRange of(Instant start, Duration duration) {
        return new TimeRange(start, start.plus(duration));
    }

    public boolean overlaps(TimeRange other) {
        return this.start.isBefore(other.end)
                && other.start.isBefore(this.end);
    }

}
