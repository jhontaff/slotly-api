package com.slotly_api.application;

import java.util.UUID;

public interface CancelBookingUseCase {

    void cancel(UUID bookingId);
}
