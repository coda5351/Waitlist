package com.waitlist.dto;

import java.time.LocalTime;

/**
 * DTO used by clients to specify a daily open/close time pair.  Keys in the
 * surrounding map are the day-of-week names (e.g. "MONDAY").
 */
public class ServiceHoursDTO {
    private LocalTime openTime;
    private LocalTime closeTime;

    public LocalTime getOpenTime() {
        return openTime;
    }

    public void setOpenTime(LocalTime openTime) {
        this.openTime = openTime;
    }

    public LocalTime getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(LocalTime closeTime) {
        this.closeTime = closeTime;
    }
}