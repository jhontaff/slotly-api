ALTER TABLE booking
    ADD COLUMN cancelled_at      TIMESTAMPTZ,
    ADD COLUMN late_cancellation BOOLEAN;

-- A booking has cancellation details if and only if it is cancelled
ALTER TABLE booking
    ADD CONSTRAINT chk_booking_cancellation CHECK (
        (status = 'CANCELLED' AND cancelled_at IS NOT NULL AND late_cancellation IS NOT NULL)
        OR
        (status <> 'CANCELLED' AND cancelled_at IS NULL AND late_cancellation IS NULL)
    );
