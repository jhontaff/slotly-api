package com.slotly_api.application;

import com.slotly_api.domain.Booking;
import com.slotly_api.domain.BookingRepository;
import com.slotly_api.domain.BookingRequest;
import com.slotly_api.domain.ProviderRepository;
import com.slotly_api.domain.exceptions.SlotUnavailableException;
import java.time.Clock;
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
        Booking booking = Booking.create(request, clock);

        providers.lockForUpdate(request.providerId());

        if (bookings.existsOverlapping(request.providerId(), booking.getTimeRange())) {
            throw new SlotUnavailableException("The slot is no longer available");
        }
        bookings.save(booking);

        return booking;
    }
}