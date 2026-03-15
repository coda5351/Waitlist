-- Add a column to store the Firebase access token for an entry.
-- This value is only used server-side and is not exposed in the EntryDTO.

ALTER TABLE entries
    ADD COLUMN firebase_access_token VARCHAR(512);
