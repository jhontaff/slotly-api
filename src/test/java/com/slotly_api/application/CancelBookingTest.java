package com.slotly_api.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.slotly_api.TestcontainersConfiguration;
import com.slotly_api.domain.Booking;
import com.slotly_api.domain.BookingRepository;
import com.slotly_api.domain.BookingRequest;
import com.slotly_api.domain.BookingStatus;
import com.slotly_api.domain.Cancellation;
import com.slotly_api.domain.exceptions.BookingNotFoundException;
import com.slotly_api.domain.exceptions.SlotUnavailableException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CancelBookingTest {

    @Autowired
    private BookSlotUseCase bookSlot;

    @Autowired
    private CancelBookingUseCase cancelBooking;

    @Autowired
    private BookingRepository bookings;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID businessId;
    private UUID providerId;
    private Instant tomorrow;

    @BeforeEach
    void createProvider() {
        businessId = UUID.randomUUID();
        providerId = UUID.randomUUID();
        tomorrow = Instant.now().plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.HOURS);
        jdbcTemplate.update("insert into provider (id, business_id) values (?, ?)", providerId, businessId);
    }

    private BookingRequest requestStartingAt(Instant start) {
        return new BookingRequest(
                businessId, providerId, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID().toString(), start, Duration.ofMinutes(30));
    }

    private Cancellation storedCancellationOf(Booking booking) {
        return bookings.findById(booking.getId()).orElseThrow().getCancellation().orElseThrow();
    }

    @Test
    void theSlotStaysTakenWhileTheBookingIsActive() {
        bookSlot.book(requestStartingAt(tomorrow));
        BookingRequest another = requestStartingAt(tomorrow);

        assertThatThrownBy(() -> bookSlot.book(another))
                .isInstanceOf(SlotUnavailableException.class);
    }

    @Test
    void cancellingFreesTheSlotForAnotherBooking() {
        Booking first = bookSlot.book(requestStartingAt(tomorrow));

        cancelBooking.cancel(first.getId());
        Booking second = bookSlot.book(requestStartingAt(tomorrow));

        assertThat(bookings.findById(first.getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CANCELLED);
        assertThat(second.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void cancellingWellBeforeTheStartIsStoredAsOnTime() {
        Booking booking = bookSlot.book(requestStartingAt(tomorrow));

        cancelBooking.cancel(booking.getId());

        assertThat(storedCancellationOf(booking).late()).isFalse();
    }

    @Test
    void cancellingCloseToTheStartIsStoredAsLate() {
        Instant inOneHour = Instant.now().plus(Duration.ofHours(1)).truncatedTo(ChronoUnit.MINUTES);
        Booking booking = bookSlot.book(requestStartingAt(inOneHour));

        cancelBooking.cancel(booking.getId());

        assertThat(storedCancellationOf(booking).late()).isTrue();
    }

    @Test
    void cancellingAnUnknownBookingFails() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> cancelBooking.cancel(unknown))
                .isInstanceOf(BookingNotFoundException.class);
    }
}
