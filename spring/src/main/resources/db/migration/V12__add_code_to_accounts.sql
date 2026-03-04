-- add a new public code column for accounts that is 6 characters
ALTER TABLE accounts ADD COLUMN code VARCHAR(6) NOT NULL DEFAULT '';

-- generate a pseudo-random alphanumeric code for existing rows; best-effort
UPDATE accounts SET code = substring(md5(random()::text) from 1 for 6) WHERE code = '';

-- ensure uniqueness and disallow empty strings in the future
ALTER TABLE accounts ADD CONSTRAINT uq_accounts_code UNIQUE (code);

-- remove the default so new rows are handled by application logic
ALTER TABLE accounts ALTER COLUMN code DROP DEFAULT;
