package com.slotly_api.domain.exceptions;

public class BookingNotStartedException extends RuntimeException {
    public BookingNotStartedException(String message) {
        super(message);
    }
}
