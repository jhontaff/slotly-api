package com.slotly_api.domain;

import com.slotly_api.domain.exceptions.BookingAlreadyStartedException;
import com.slotly_api.domain.exceptions.BookingNotStartedException;
import com.slotly_api.domain.exceptions.InvalidTransitionException;
import com.slotly_api.domain.exceptions.StartNotInFutureException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class Booking {

    private final UUID id;
    private final UUID businessId;
    private final UUID providerId;
    private final UUID customerId;
    private final UUID offeringId;
    private final String idempotencyKey;

    private TimeRange timeRange;
    private BookingStatus status;
    private Cancellation cancellation; // present only when status is CANCELLED

    // One parameter per field: the aggregate has 9 fields and this is the single place that sets them all.
    @SuppressWarnings("java:S107")
    private Booking(UUID id, UUID businessId, UUID providerId, UUID customerId, UUID offeringId,
                    String idempotencyKey, TimeRange timeRange, BookingStatus status,
                    Cancellation cancellation) {
        this.id = Objects.requireNonNull(id, "id");
        this.businessId = Objects.requireNonNull(businessId, "businessId");
        this.providerId = Objects.requireNonNull(providerId, "providerId");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.offeringId = Objects.requireNonNull(offeringId, "offeringId");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        this.timeRange = Objects.requireNonNull(timeRange, "timeRange");
        this.status = Objects.requireNonNull(status, "status");
        this.cancellation = cancellation;

        if ((status == BookingStatus.CANCELLED) != (cancellation != null)) {
            throw new IllegalArgumentException(
                    "A booking has cancellation details if and only if it is cancelled");
        }
    }

    public static Booking create(BookingRequest request, Clock clock) {
        requireFuture(request.start(), clock);
        return new Booking(
                UUID.randomUUID(),
                request.businessId(),
                request.providerId(),
                request.customerId(),
                request.offeringId(),
                request.idempotencyKey(),
                TimeRange.of(request.start(), request.duration()),
                BookingStatus.CONFIRMED,
                null);
    }

    @SuppressWarnings("java:S107")
    public static Booking restore(UUID id, UUID businessId, UUID providerId, UUID customerId,
                                  UUID offeringId, String idempotencyKey, TimeRange timeRange,
                                  BookingStatus status, Cancellation cancellation) {
        return new Booking(id, businessId, providerId, customerId, offeringId,
                idempotencyKey, timeRange, status, cancellation);
    }


    public void cancel(Clock clock, Duration lateWindow) {
        Objects.requireNonNull(lateWindow, "lateWindow");
        requireConfirmed("cancelled");
        requireNotStarted("cancel", clock);

        Instant now = clock.instant();
        boolean late = now.isAfter(timeRange.start().minus(lateWindow));

        status = BookingStatus.CANCELLED;
        cancellation = new Cancellation(now, late);
    }

    public void complete(Clock clock) {
        requireConfirmed("completed");
        requireStarted("complete", clock);
        status = BookingStatus.COMPLETED;
    }

    public void markNoShow(Clock clock) {
        requireConfirmed("marked as no-show");
        requireStarted("mark as no-show", clock);
        status = BookingStatus.NO_SHOW;
    }

    public void reschedule(Instant newStart, Clock clock) {
        requireConfirmed("rescheduled");
        requireNotStarted("reschedule", clock);
        requireFuture(newStart, clock);
        timeRange = TimeRange.of(newStart, timeRange.duration());
    }

    public boolean isSameRequest(BookingRequest request) {
        return providerId.equals(request.providerId())
                && offeringId.equals(request.offeringId())
                && timeRange.equals(TimeRange.of(request.start(), request.duration()));
    }

    private static void requireFuture(Instant start, Clock clock) {
        if (!start.isAfter(clock.instant())) {
            throw new StartNotInFutureException("Start time must be in the future");
        }
    }

    private void requireConfirmed(String action) {
        if (status != BookingStatus.CONFIRMED) {
            throw new InvalidTransitionException("Only confirmed bookings can be " + action);
        }
    }

    private void requireStarted(String action, Clock clock) {
        if (timeRange.start().isAfter(clock.instant())) {
            throw new BookingNotStartedException("Cannot " + action + " a booking that has not started");
        }
    }

    private void requireNotStarted(String action, Clock clock) {
        if (!timeRange.start().isAfter(clock.instant())) {
            throw new BookingAlreadyStartedException("Cannot " + action + " a booking that has already started");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public UUID getProviderId() {
        return providerId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getOfferingId() {
        return offeringId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public TimeRange getTimeRange() {
        return timeRange;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Optional<Cancellation> getCancellation() {
        return Optional.ofNullable(cancellation);
    }
}
