package com.slotly_api.application;

import com.slotly_api.domain.Booking;
import com.slotly_api.domain.BookingRepository;
import com.slotly_api.domain.BookingRequest;
import com.slotly_api.domain.ProviderRepository;
import com.slotly_api.domain.exceptions.IdempotencyKeyReusedException;
import com.slotly_api.domain.exceptions.SlotUnavailableException;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class BookSlotService implements BookSlotUseCase {

    private final BookingRepository bookings;
    private final ProviderRepository providers;
    private final Clock clock;

    BookSlotService(BookingRepository bookings, ProviderRepository providers, Clock clock) {
        this.bookings = bookings;
        this.providers = providers;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Booking book(BookingRequest request) {
        // Validate first: domain rules need no database and no lock
        Booking booking = Booking.create(request, clock);

        // Bookings for the same provider wait here, one at a time, until this transaction ends
        providers.lockForUpdate(request.providerId());

        // A retry must get its original booking back, not collide with it
        Optional<Booking> previous = bookings.findByIdempotencyKey(
                request.businessId(), request.customerId(), request.idempotencyKey());
        if (previous.isPresent()) {
            return replay(previous.get(), request);
        }

        if (bookings.existsOverlapping(request.providerId(), booking.getTimeRange())) {
            throw new SlotUnavailableException("The slot is no longer available");
        }

        bookings.save(booking);

        return booking;
    }

    private static Booking replay(Booking previous, BookingRequest request) {
        if (!previous.isSameRequest(request)) {
            throw new IdempotencyKeyReusedException();
        }
        return previous;
    }
}