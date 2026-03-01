package com.waitlist.controller;

import org.springframework.http.ResponseEntity;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hello")
public class HelloController {

    @GetMapping
    public ResponseEntity<Map<String, String>> satHellow() {
        return ResponseEntity.ok(Map.of("message", "Hello World"));
    }
}
