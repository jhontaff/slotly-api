package com.slotly_api.domain;

import java.util.UUID;

public interface BookingRepository {

    boolean existsOverlapping(UUID providerId, TimeRange range);

    void save(Booking booking);
}
