-- add account_id column to entries
ALTER TABLE entries ADD COLUMN account_id BIGINT;

-- populate existing rows with the first account (if any); if no account exists
-- this leaves entries.nullable initially so the update can succeed regardless.
UPDATE entries SET account_id = (SELECT id FROM accounts ORDER BY id LIMIT 1) WHERE account_id IS NULL;

-- enforce foreign key relationship
ALTER TABLE entries ADD CONSTRAINT fk_entries_account FOREIGN KEY (account_id) REFERENCES accounts(id);

-- disallow nulls going forward (will fail if any rows still NULL)
ALTER TABLE entries ALTER COLUMN account_id SET NOT NULL;
