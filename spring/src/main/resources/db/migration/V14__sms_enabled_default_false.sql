-- change default of sms_enabled to false for new accounts
ALTER TABLE accounts ALTER COLUMN sms_enabled SET DEFAULT false;

-- optionally convert existing rows that still rely on the old default
UPDATE accounts SET sms_enabled = false WHERE sms_enabled IS TRUE;