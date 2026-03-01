-- Add active flag for soft deletes
ALTER TABLE entries
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

-- ensure existing rows marked active
UPDATE entries SET active = TRUE;