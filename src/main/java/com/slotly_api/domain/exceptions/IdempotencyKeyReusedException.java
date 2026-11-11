package com.slotly_api.domain.exceptions;

public class IdempotencyKeyReusedException extends RuntimeException {
    public IdempotencyKeyReusedException() {
        super("This idempotency key was already used for a different booking request");
    }
}