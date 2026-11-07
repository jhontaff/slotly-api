package com.slotly_api.application;

import com.slotly_api.domain.Booking;
import com.slotly_api.domain.BookingRequest;

public interface BookSlotUseCase {

    Booking book(BookingRequest request);
}