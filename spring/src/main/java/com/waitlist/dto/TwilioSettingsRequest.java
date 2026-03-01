package com.waitlist.dto;

/**
 * Carries Twilio credential settings when administrators update account configuration.
 */
public class TwilioSettingsRequest {
    private String accountSid;
    private String authToken;
    // optional flag that allows administrators to disable SMS delivery
    private Boolean smsEnabled;

    public String getAccountSid() {
        return accountSid;
    }

    public void setAccountSid(String accountSid) {
        this.accountSid = accountSid;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public Boolean getSmsEnabled() {
        return smsEnabled;
    }

    public void setSmsEnabled(Boolean smsEnabled) {
        this.smsEnabled = smsEnabled;
    }
}
