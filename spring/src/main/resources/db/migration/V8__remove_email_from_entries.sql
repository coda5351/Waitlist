-- remove email column now that we rely exclusively on phone numbers
ALTER TABLE entries
    DROP COLUMN email;