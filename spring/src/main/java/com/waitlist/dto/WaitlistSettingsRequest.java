package com.waitlist.dto;

import java.time.LocalDateTime;

/**
 * Carries a thin set of properties used by administrators to configure
 * the waitlist behaviour for an account.  All fields are optional so that
 * the controller can be used in a PATCH-style update (update only what is
 * provided).
 */
public class WaitlistSettingsRequest {
    private Boolean waitlistEnabled;
    private LocalDateTime waitlistOpenTime;
    private LocalDateTime waitlistCloseTime;

    // optional service hours map; keys are day-of-week string names
    private java.util.Map<String, ServiceHoursDTO> serviceHours;

    public Boolean getWaitlistEnabled() {
        return waitlistEnabled;
    }

    public void setWaitlistEnabled(Boolean waitlistEnabled) {
        this.waitlistEnabled = waitlistEnabled;
    }

    public LocalDateTime getWaitlistOpenTime() {
        return waitlistOpenTime;
    }

    public void setWaitlistOpenTime(LocalDateTime waitlistOpenTime) {
        this.waitlistOpenTime = waitlistOpenTime;
    }

    public LocalDateTime getWaitlistCloseTime() {
        return waitlistCloseTime;
    }

    public void setWaitlistCloseTime(LocalDateTime waitlistCloseTime) {
        this.waitlistCloseTime = waitlistCloseTime;
    }

    public java.util.Map<String, ServiceHoursDTO> getServiceHours() {
        return serviceHours;
    }

    public void setServiceHours(java.util.Map<String, ServiceHoursDTO> serviceHours) {
        this.serviceHours = serviceHours;
    }
}
