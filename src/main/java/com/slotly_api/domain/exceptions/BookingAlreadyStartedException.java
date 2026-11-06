package com.slotly_api.domain.exceptions;

public class BookingAlreadyStartedException extends RuntimeException {
    public BookingAlreadyStartedException(String message) {
        super(message);
    }
}
