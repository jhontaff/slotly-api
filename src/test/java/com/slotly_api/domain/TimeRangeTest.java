package com.slotly_api.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

public class TimeRangeTest {

    private static final Instant TEN_AM = Instant.parse("2026-12-01T10:00:00Z");
    private static TimeRange range(int startMinute, int endMinute) {
        return new TimeRange(
                TEN_AM.plus(Duration.ofMinutes(startMinute)),
                TEN_AM.plus(Duration.ofMinutes(endMinute)));
    }

    @Test
    void rejectsStartEqualToEnd() {
        assertThatThrownBy(() -> new TimeRange(TEN_AM, TEN_AM))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Start must be before end: [%s, %s)".formatted(TEN_AM, TEN_AM));
    }

    @Test
    void ofCalculatesEndFromDuration() {
        TimeRange range = TimeRange.of(TEN_AM, Duration.ofMinutes(30));

        assertThat(range.end()).isEqualTo(TEN_AM.plus(Duration.ofMinutes(30)));
    }

    @Test
    void consecutiveRangesDoNotOverlap() {
        assertThat(range(0, 30).overlaps(range(30, 60))).isFalse();
    }

    @Test
    void partialOverlapIsDetectedInBothDirections() {
        assertThat(range(0, 45).overlaps(range(30, 60))).isTrue();
        assertThat(range(30, 60).overlaps(range(0, 45))).isTrue();
    }

    @Test
    void rangeContainedInAnotherOverlaps() {
        assertThat(range(0, 60).overlaps(range(15, 30))).isTrue();
    }


}
