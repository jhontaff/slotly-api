package com.slotly_api.application;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "slotly.cancellation")
public record CancellationPolicy(Duration lateWindow) {

    public CancellationPolicy {
        Objects.requireNonNull(lateWindow, "slotly.cancellation.late-window must be set");
        if (lateWindow.isNegative()) {
            throw new IllegalArgumentException("slotly.cancellation.late-window cannot be negative");
        }
    }
}
