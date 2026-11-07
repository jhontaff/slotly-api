package com.slotly_api.domain.exceptions;

public class SlotUnavailableException extends RuntimeException {
    public SlotUnavailableException(String message) {
        super(message);
    }
}
