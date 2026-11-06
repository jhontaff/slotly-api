package com.slotly_api.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record BookingRequest(

    UUID businessId,
    UUID providerId,
    UUID customerId,
    UUID offeringId,
    String idempotencyKey,
    Instant start,
    Duration duration
) {
    public BookingRequest {
        Objects.requireNonNull(businessId, "Business ID cannot be null");
        Objects.requireNonNull(providerId, "Provider ID cannot be null");
        Objects.requireNonNull(customerId, "Customer ID cannot be null");
        Objects.requireNonNull(offeringId, "Offering ID cannot be null");
        Objects.requireNonNull(idempotencyKey, "Idempotency key cannot be null");
        Objects.requireNonNull(duration, "Duration cannot be null");
        Objects.requireNonNull(start, "Start time cannot be null");
        if (idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency key cannot be blank");
        }
    }
}
