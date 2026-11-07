-- Allows a GiST index to compare UUIDs with "=" (needed by the exclusion constraint)
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- A provider can never have two non-cancelled bookings whose time ranges overlap
ALTER TABLE booking
    ADD CONSTRAINT booking_no_overlap
    EXCLUDE USING gist (
        provider_id WITH =,
        tstzrange(start_at, end_at, '[)') WITH &&
    )
    WHERE (status <> 'CANCELLED');