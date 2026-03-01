package com.waitlist.model;

import jakarta.persistence.Embeddable;
import java.time.LocalTime;

/**
 * Embeddable representing opening/closing times (service hours) for a day.
 */
@Embeddable
public class ServiceHours {
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