package com.slotly_api.domain;

import java.time.Instant;
import java.util.Objects;

public record Cancellation(Instant cancelledAt, boolean late) {

    public Cancellation {
        Objects.requireNonNull(cancelledAt, "cancelledAt");
    }
}
