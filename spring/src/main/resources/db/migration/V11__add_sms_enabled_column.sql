-- add flag to disable/enable SMS per-account
ALTER TABLE accounts ADD COLUMN sms_enabled boolean NOT NULL DEFAULT true;