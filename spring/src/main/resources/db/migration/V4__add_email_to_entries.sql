-- add email column to waitlist entries for notification purposes
ALTER TABLE entries
    ADD COLUMN email VARCHAR(255);
