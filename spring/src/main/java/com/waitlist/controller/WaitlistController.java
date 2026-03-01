package com.waitlist.controller;

import com.waitlist.model.Account;
import com.waitlist.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/waitlists")
public class WaitlistController {

    @Autowired
    private AccountService accountService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getWaitlistStatus() {
        boolean open = accountService.isWaitlistOpen(1L);
        Account account = accountService.getAccountById(1L);
        int estimate = accountService.getEstimatedWaitMinutes();
        Map<String, Object> body = Map.of(
                "open", open,
                "enabled", account.isWaitlistEnabled(),
                "openTime", account.getWaitlistOpenTime(),
                "closeTime", account.getWaitlistCloseTime(),
                "serviceHours", account.getServiceHours(),
                "estimatedWait", estimate
        );
        return ResponseEntity.ok(body);
    }
}
