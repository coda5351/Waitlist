package com.waitlist.controller;

import com.waitlist.model.Account;
import com.waitlist.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccountById(@PathVariable Long id) {
        Account account = accountService.getAccountById(id);
        return ResponseEntity.ok(account);
    }

    @PatchMapping("/{id}/code")
    public ResponseEntity<Account> updateCode(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String code = request.get("code");
        Account account = accountService.updateCode(id, code);
        return ResponseEntity.ok(account);
    }

    @PatchMapping("/{id}/branding-color")
    public ResponseEntity<Account> updateBrandingColor(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String brandingColorCode = request.get("brandingColorCode");
        Account account = accountService.updateBrandingColor(id, brandingColorCode);
        return ResponseEntity.ok(account);
    }

    @PatchMapping("/{id}/waitlist-settings")
    public ResponseEntity<?> updateWaitlistSettings(@PathVariable Long id,
                                                     @RequestBody com.waitlist.dto.WaitlistSettingsRequest request) {
        try {
            Account account = accountService.updateWaitlistSettings(id, request);
            return ResponseEntity.ok(account);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PatchMapping("/{id}/twilio-settings")
    public ResponseEntity<?> updateTwilioSettings(@PathVariable Long id,
                                                   @RequestBody com.waitlist.dto.TwilioSettingsRequest request) {
        try {
            Account account = accountService.updateTwilioSettings(id, request);
            return ResponseEntity.ok(account);
        } catch (IllegalArgumentException ex) {
            // return a user-friendly message on bad credentials
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }


    /**
     * Fetch all configured message templates for an account.  The frontend
     * exposes these on the "Account Messages" page so administrators can edit
     * the text used in notifications.
     */
    @GetMapping("/{id}/messages")
    public ResponseEntity<java.util.Map<String, String>> getMessageTemplates(@PathVariable Long id) {
        java.util.Map<String, String> msgs = accountService.getMessageTemplates(id);
        return ResponseEntity.ok(msgs);
    }

    /**
     * Replace the message template map for an account.  Accepts a simple JSON
     * object where keys are template identifiers and values are format strings.
     */
    @PatchMapping("/{id}/messages")
    public ResponseEntity<java.util.Map<String, String>> updateMessageTemplates(@PathVariable Long id,
                                                                                @RequestBody java.util.Map<String, String> templates) {
        Account updated = accountService.updateMessageTemplates(id, templates);
        return ResponseEntity.ok(updated.getMessages());
    }
}
