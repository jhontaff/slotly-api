package com.slotly_api.application;

import com.slotly_api.domain.Booking;
import com.slotly_api.domain.BookingRepository;
import com.slotly_api.domain.exceptions.BookingNotFoundException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CancelBookingService implements CancelBookingUseCase {

    private final BookingRepository bookings;
    private final CancellationPolicy policy;
    private final Clock clock;

    CancelBookingService(BookingRepository bookings, CancellationPolicy policy, Clock clock) {
        this.bookings = bookings;
        this.policy = policy;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void cancel(UUID bookingId) {
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        // The domain decides whether it can be cancelled and whether it is late
        booking.cancel(clock, policy.lateWindow());

        bookings.update(booking);
    }
}
