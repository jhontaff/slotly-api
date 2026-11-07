package com.slotly_api.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BookingRequestTest {

    private static final UUID ANY_ID = UUID.randomUUID();
    private static final Instant START = Instant.parse("2026-12-01T10:00:00Z");
    private static final Duration HALF_HOUR = Duration.ofMinutes(30);

    @Test
    void rejectsNullProviderId() {
        assertThatThrownBy(() -> new BookingRequest(
                ANY_ID, null, ANY_ID, ANY_ID,
                "key-1", START, HALF_HOUR))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsBlankIdempotencyKey() {
        assertThatThrownBy(() -> new BookingRequest(
                ANY_ID, ANY_ID, ANY_ID, ANY_ID,
                "   ", START, HALF_HOUR))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
