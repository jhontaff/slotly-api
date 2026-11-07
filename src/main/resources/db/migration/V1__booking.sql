CREATE TABLE booking (
                         id              UUID         PRIMARY KEY,
                         business_id     UUID         NOT NULL,
                         provider_id     UUID         NOT NULL,
                         customer_id     UUID         NOT NULL,
                         offering_id     UUID         NOT NULL,
                         idempotency_key VARCHAR(255) NOT NULL,
                         start_at        TIMESTAMPTZ  NOT NULL,
                         end_at          TIMESTAMPTZ  NOT NULL,
                         status          VARCHAR(20)  NOT NULL,
                         version         BIGINT       NOT NULL DEFAULT 0,

                         CONSTRAINT chk_booking_time_range
                             CHECK (start_at < end_at),

                         CONSTRAINT chk_booking_status
                             CHECK (status IN ('CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW')),

                         CONSTRAINT uq_booking_idempotency
                             UNIQUE (business_id, customer_id, idempotency_key)
);