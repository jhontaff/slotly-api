package com.slotly_api.domain;

import com.slotly_api.domain.exceptions.BookingAlreadyStartedException;
import com.slotly_api.domain.exceptions.BookingNotStartedException;
import com.slotly_api.domain.exceptions.InvalidTransitionException;
import com.slotly_api.domain.exceptions.StartNotInFutureException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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

    private Booking(UUID id, BookingRequest request, TimeRange timeRange, BookingStatus status) {
        this.id = id;
        this.businessId = request.businessId();
        this.providerId = request.providerId();
        this.customerId = request.customerId();
        this.offeringId = request.offeringId();
        this.idempotencyKey = request.idempotencyKey();
        this.timeRange = timeRange;
        this.status = status;
    }

    public static Booking create(BookingRequest request, Clock clock) {
        requireFuture(request.start(), clock);
        TimeRange timeRange = TimeRange.of(request.start(), request.duration());
        return new Booking(UUID.randomUUID(), request, timeRange, BookingStatus.CONFIRMED);

    }

    public void cancel(Clock clock) {
        requireConfirmed("cancelled");
        requireNotStarted("cancel", clock);
        status = BookingStatus.CANCELLED;
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
}
