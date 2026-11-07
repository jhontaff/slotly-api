package com.slotly_api.domain.exceptions;

import java.util.UUID;

public class ProviderNotFoundException extends RuntimeException {
    public ProviderNotFoundException(UUID providerId) {
        super("Provider not found: " + providerId);
    }
}