-- Add waitlist schedule columns to accounts table
ALTER TABLE accounts
    ADD COLUMN waitlist_enabled BOOLEAN DEFAULT FALSE,
    ADD COLUMN waitlist_open_time TIMESTAMP,
    ADD COLUMN waitlist_close_time TIMESTAMP;
