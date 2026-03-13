-- add a source column to entries to track whether the entry was created by the web app or the flutter app
ALTER TABLE entries ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'WEB';

-- backfill any existing rows (including those created by earlier versions that stored lowercase)
UPDATE entries SET source = 'WEB' WHERE source IS NULL OR LOWER(source) = 'web';

-- remove the default so future inserts come from application logic
ALTER TABLE entries ALTER COLUMN source DROP DEFAULT;
