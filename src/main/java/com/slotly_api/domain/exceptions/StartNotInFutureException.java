package com.slotly_api.domain.exceptions;

public class StartNotInFutureException extends RuntimeException {
    public StartNotInFutureException(String message) {
        super(message);
    }
}
