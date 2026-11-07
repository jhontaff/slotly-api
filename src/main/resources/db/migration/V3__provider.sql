CREATE TABLE provider (
                          id          UUID PRIMARY KEY,
                          business_id UUID NOT NULL
);

ALTER TABLE booking
    ADD CONSTRAINT fk_booking_provider
        FOREIGN KEY (provider_id) REFERENCES provider (id);