package com.waitlist.model;

import com.waitlist.service.MessageTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MessageTemplateTest {

    @Test
    void enumValues_shouldHaveKeyAndDefault() {
        for (MessageTemplate mt : MessageTemplate.values()) {
            assertNotNull(mt.getKey());
            assertNotNull(mt.getDefaultTemplate());
            assertFalse(mt.getKey().isEmpty());
            assertFalse(mt.getDefaultTemplate().isEmpty());
        }
    }
}
