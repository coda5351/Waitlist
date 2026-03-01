package com.waitlist.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class HelloControllerTest {
    private final HelloController controller = new HelloController();

    @Test
    void satHellow_shouldReturnHelloMessage() {
        ResponseEntity<Map<String, String>> res = controller.satHellow();
        assertEquals("Hello World", res.getBody().get("message"));
    }
}
