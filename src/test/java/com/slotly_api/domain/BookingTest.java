package com.slotly_api.domain;

import com.slotly_api.domain.exceptions.BookingAlreadyStartedException;
import com.slotly_api.domain.exceptions.BookingNotStartedException;
import com.slotly_api.domain.exceptions.InvalidTransitionException;
import com.slotly_api.domain.exceptions.StartNotInFutureException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BookingTest {

    private static final Instant NOW = Instant.parse("2026-12-01T08:00:00Z");
    private static final Instant START = Instant.parse("2026-12-01T10:00:00Z");
    private static final Duration HALF_HOUR = Duration.ofMinutes(30);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Duration LATE_WINDOW = Duration.ofHours(2);

    private static BookingRequest request(Instant start) {
        return new BookingRequest(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "key-1", start, HALF_HOUR);
    }

    private static Clock clockAt(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    private static Booking confirmedBooking() {
        return Booking.create(request(START), CLOCK);
    }

    @Test
    void newBookingIsConfirmedAndEndIsStartPlusDuration() {
        Booking booking = Booking.create(request(START), CLOCK);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getTimeRange()).isEqualTo(new TimeRange(START, START.plus(HALF_HOUR)));
    }

    @Test
    void rejectsStartEqualToNow() {
        BookingRequest request = request(NOW);
        assertThatThrownBy(() -> Booking.create(request, CLOCK))
                .isInstanceOf(StartNotInFutureException.class);
    }

    @Test
    void rejectsStartInThePast() {
        BookingRequest request = request(NOW.minusSeconds(1));
        assertThatThrownBy(() -> Booking.create(request, CLOCK))
                .isInstanceOf(StartNotInFutureException.class);
    }

    @Test
    void confirmedBookingCanBeCancelledJustBeforeStart() {
        Booking booking = confirmedBooking();

        booking.cancel(clockAt(START.minusSeconds(1)), LATE_WINDOW);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cannotCancelAtStartTime() {
        Booking booking = confirmedBooking();
        Clock atStart = clockAt(START);

        assertThatThrownBy(() -> booking.cancel(atStart, LATE_WINDOW))
                .isInstanceOf(BookingAlreadyStartedException.class);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void cannotCancelTwice() {
        Booking booking = confirmedBooking();
        booking.cancel(CLOCK, LATE_WINDOW);

        assertThatThrownBy(() -> booking.cancel(CLOCK, LATE_WINDOW))
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void cancellingExactlyAtTheWindowLimitIsOnTime() {
        Booking booking = confirmedBooking(); // now 08:00, start 10:00: exactly 2 hours before

        booking.cancel(CLOCK, LATE_WINDOW);

        assertThat(booking.getCancellation()).contains(new Cancellation(NOW, false));
    }

    @Test
    void cancellingInsideTheWindowIsLate() {
        Booking booking = confirmedBooking();
        Instant oneHourBefore = START.minus(Duration.ofHours(1));

        booking.cancel(clockAt(oneHourBefore), LATE_WINDOW);

        assertThat(booking.getCancellation()).contains(new Cancellation(oneHourBefore, true));
    }

    @Test
    void activeBookingHasNoCancellationDetails() {
        assertThat(confirmedBooking().getCancellation()).isEmpty();
    }

    @Test
    void restoreRejectsACancelledBookingWithoutCancellationDetails() {
        TimeRange range = new TimeRange(START, START.plus(HALF_HOUR));

        assertThatThrownBy(() -> Booking.restore(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "key-1", range, BookingStatus.CANCELLED, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void confirmedBookingCanBeCompletedAtStartTime() {
        Booking booking = confirmedBooking();

        booking.complete(clockAt(START));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
    }

    @Test
    void cannotCompleteBeforeStart() {
        Booking booking = confirmedBooking();
        Clock justBefore = clockAt(START.minusSeconds(1));

        assertThatThrownBy(() -> booking.complete(justBefore))
                .isInstanceOf(BookingNotStartedException.class);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void cannotCompleteCancelledBooking() {
        Booking booking = confirmedBooking();
        booking.cancel(CLOCK, LATE_WINDOW);
        Clock atStart = clockAt(START);

        assertThatThrownBy(() -> booking.complete(atStart))
                .isInstanceOf(InvalidTransitionException.class);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void confirmedBookingCanBeMarkedNoShowAtStartTime() {
        Booking booking = confirmedBooking();
        booking.markNoShow(clockAt(START));
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.NO_SHOW);
    }

    @Test
    void cannotMarkNoShowBeforeStart() {
        Booking booking = confirmedBooking();
        Clock justBefore = clockAt(START.minusSeconds(1));
        assertThatThrownBy(() -> booking.markNoShow(justBefore))
                .isInstanceOf(BookingNotStartedException.class);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void cannotMarkNoShowCancelledBooking() {
        Booking booking = confirmedBooking();
        booking.cancel(CLOCK, LATE_WINDOW);
        Clock atStart = clockAt(START);

        assertThatThrownBy(() -> booking.markNoShow(atStart))
                .isInstanceOf(InvalidTransitionException.class);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void reschedulingKeepsTheDuration() {
        Booking booking = confirmedBooking();
        Instant newStart = START.plus(Duration.ofHours(3));

        booking.reschedule(newStart, CLOCK);

        assertThat(booking.getTimeRange()).isEqualTo(new TimeRange(newStart, newStart.plus(HALF_HOUR)));
    }

    @Test
    void cannotRescheduleAtStartTime() {
        Booking booking = confirmedBooking();
        Instant newStart = START.plus(Duration.ofDays(1));
        Clock atStart = clockAt(START);

        assertThatThrownBy(() -> booking.reschedule(newStart, atStart))
                .isInstanceOf(BookingAlreadyStartedException.class);
        assertThat(booking.getTimeRange().start()).isEqualTo(START);
    }

    @Test
    void cannotRescheduleToThePast() {
        Booking booking = confirmedBooking();
        Instant past = NOW.minusSeconds(1);

        assertThatThrownBy(() -> booking.reschedule(past, CLOCK))
                .isInstanceOf(StartNotInFutureException.class);
        assertThat(booking.getTimeRange().start()).isEqualTo(START);
    }

    @Test
    void cannotRescheduleCancelledBooking() {
        Booking booking = confirmedBooking();
        booking.cancel(CLOCK, LATE_WINDOW);
        Instant newStart = START.plus(Duration.ofHours(3));

        assertThatThrownBy(() -> booking.reschedule(newStart, CLOCK))
                .isInstanceOf(InvalidTransitionException.class);
    }

}
