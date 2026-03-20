package com.waitlist.controller;

import com.waitlist.exception.ResourceNotFoundException;
import com.waitlist.model.Account;
import com.waitlist.model.Entry;
import com.waitlist.service.AccountService;
import com.waitlist.service.EntryService;

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

    @Autowired
    private EntryService entryService;
    
    @GetMapping("{code}/status")
    public ResponseEntity<Map<String, Object>> getWaitlistStatus(@PathVariable String code) {
        Account account = accountService.getAccountByCode(code);
        boolean open = accountService.isWaitlistOpen(account.getId());
        int estimate = accountService.getEstimatedWaitMinutes(account.getId());
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("open", open);
        body.put("enabled", account.isWaitlistEnabled());
        if (account.getWaitlistOpenTime() != null) {
            body.put("openTime", account.getWaitlistOpenTime());
        }
        if (account.getWaitlistCloseTime() != null) {
            body.put("closeTime", account.getWaitlistCloseTime());
        }
        if (account.getServiceHours() != null) {
            body.put("serviceHours", account.getServiceHours());
        }
        body.put("estimatedWait", estimate);
        body.put("entries", entryService.getAllActiveEntryDtosForAccount(account.getId()));
        return ResponseEntity.ok(body);
    }

    @GetMapping("entry/{code}/status")
    public ResponseEntity<Map<String, Object>> getEntryWaitlistStatus(@PathVariable String code) {
        Entry entry = entryService.resolve(code);

        if (entry.isCalled()) {
            throw new ResourceNotFoundException("Entry not found: " + code) ;
        }

        boolean open = accountService.isWaitlistOpen(entry.getAccount().getId());
        int estimate = accountService.getEstimatedWaitMinutes(entry.getAccount().getId());
        Map<String, Object> body = Map.of(
                "open", open,
                "estimatedWait", estimate,
                "entries", entryService.getAllActiveEntryDtosForAccount(entry.getAccount().getId())
        );
        return ResponseEntity.ok(body);
    }   
}
