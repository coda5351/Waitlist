-- add Twilio configuration fields to accounts table
ALTER TABLE accounts
    ADD COLUMN twilio_account_sid varchar(200),
    ADD COLUMN twilio_auth_token varchar(200);
