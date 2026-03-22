package com.waitlist.service;

import com.waitlist.model.EventName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FirebaseServiceTest {

    @Test
    void sendFirebaseNotification_returnsFalse_whenNotInitialized() {
        FirebaseService svc = new FirebaseService("");
        assertFalse(svc.sendFirebaseNotification(EventName.NOTIFIED_ENTRY, "token123", "hello"));
    }

    @Test
    void sendFirebaseNotification_returnsFalse_whenTokenMissing_evenIfInitialized() throws Exception {
        FirebaseService svc = new FirebaseService("");

        java.lang.reflect.Field initialized = FirebaseService.class.getDeclaredField("initialized");
        initialized.setAccessible(true);
        initialized.setBoolean(svc, true);

        assertFalse(svc.sendFirebaseNotification(EventName.NOTIFIED_ENTRY, null, "hello"));
        assertFalse(svc.sendFirebaseNotification(EventName.NOTIFIED_ENTRY, "  ", "hello"));
    }
}
