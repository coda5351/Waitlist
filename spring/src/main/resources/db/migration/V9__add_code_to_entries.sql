-- add a new public code column for entries that is 6 characters
ALTER TABLE entries ADD COLUMN code VARCHAR(6) NOT NULL DEFAULT '';

-- generate a pseudo-random alphanumeric code for existing rows; this is a best-effort
UPDATE entries SET code = substring(md5(random()::text) from 1 for 6) WHERE code = '';

-- ensure uniqueness and disallow empty strings in the future
ALTER TABLE entries ADD CONSTRAINT uq_entries_code UNIQUE (code);

-- remove the default so new rows are handled by application logic
ALTER TABLE entries ALTER COLUMN code DROP DEFAULT;
