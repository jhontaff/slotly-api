package com.slotly_api.domain;

import com.slotly_api.domain.exceptions.BookingNotFoundException;
import com.slotly_api.domain.exceptions.SlotUnavailableException;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository {

    boolean existsOverlapping(UUID providerId, TimeRange range);

    /**
     * Saves a new booking.
     *
     * @throws SlotUnavailableException if another non-cancelled booking of the same
     *         provider overlaps its time range, even if it was saved concurrently
     */
    void save(Booking booking);

    Optional<Booking> findById(UUID id);

    /**
     * Saves the new status, time range and cancellation details of an existing booking.
     *
     * @throws BookingNotFoundException if the booking does not exist
     * @throws SlotUnavailableException if the new time range overlaps another
     *         non-cancelled booking of the same provider
     */
    void update(Booking booking);
}
