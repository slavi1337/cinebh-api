ALTER TABLE bookings
    ADD COLUMN ticket_code uuid DEFAULT gen_random_uuid();

ALTER TABLE bookings
    ALTER COLUMN ticket_code SET NOT NULL;

ALTER TABLE bookings
    ADD CONSTRAINT uk_bookings_ticket_code UNIQUE (ticket_code);
