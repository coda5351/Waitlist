package com.waitlist.service;

import com.twilio.exception.ApiException;
import com.waitlist.model.Entry;
import com.waitlist.model.EntrySource;
import com.waitlist.model.EventName;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private SmsService smsService;

    @Autowired
    private FirebaseService firebaseService;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private AccountService accountService;

    @Autowired
    private org.springframework.core.env.Environment environment;

    @org.springframework.beans.factory.annotation.Value("${twilio.dev-target:}")
    private String devTarget;

    @org.springframework.beans.factory.annotation.Value("${twilio.use-dev-number:false}")
    private boolean useDevNumber;

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public Map<String, Object> sendFormattedNotification(Entry entry, EventName eventName) {
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", true);
        if (entry == null || entry.getAccount() == null) {
            response.put("success", false);
            response.put("message", "Entry or account information is missing; cannot send notification");
            return response;
        }
        long accountId = entry.getAccount().getId();
        String template;
        String msgString;
        switch (eventName) {
            case NOTIFIED_ENTRY:
                template = accountService.getMessageTemplate(accountId, MessageTemplate.TABLE_READY.getKey());
                if (template == null) {
                    template = MessageTemplate.TABLE_READY.getDefaultTemplate();
                }
                msgString = String.format(template,
                        entry.getName(),
                        entry.getName(),
                        entry.getPhone(),
                        entry.getPartySize());
                break;
            case NEW_ENTRY:
                template = accountService.getMessageTemplate(accountId, MessageTemplate.NEW_ENTRY.getKey());
                if (template == null) {
                    template = MessageTemplate.NEW_ENTRY.getDefaultTemplate();
                }
                msgString = String.format(template,
                        entry.getName(),
                        frontendUrl + "/waitlist/view",
                        entry.getCode(),
                        entry.getName(),
                        entry.getPhone(),
                        entry.getPartySize());
                break;
            default:
                response.replace("success", false);
                response.put("message", "Unknown event name: " + eventName);
                return response;
        }
        
        if (entry.getSource() == EntrySource.WEB) {
            return sendSmsNotification(entry, msgString);
        } else if (entry.getSource() == EntrySource.ANDROID_FLUTTER) {
            String token = entry.getFirebaseAccessToken();
            if (token == null || token.isBlank() || !firebaseService.sendFirebaseNotification(eventName, token, msgString)) {
                response.replace("success", false);
                response.put("message", "Failed to send Firebase notification");
            }
            if (response.get("success") instanceof Boolean && !(Boolean) response.get("success")) {
                return sendSmsNotification(entry, msgString);
            } else {
                response.put("message", "Firebase notification sent successfully");
            }
        }
        return response;
    }

    public Map<String, Object> sendSmsNotification(Entry entry, String message) {
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", true);
        if (entry == null || entry.getAccount() == null) {
            response.put("success", false);
            response.put("message", "Entry or account information is missing; cannot send notification");
            return response;
        }
        try {
            if (!entry.getAccount().isSmsEnabled()) {
                response.replace("success", false);
                response.put("message", "SMS notifications are disabled for this account");
            } else if (entry.getPhone() != null && !entry.getPhone().isBlank() && isValidUsNumber(entry.getPhone())) {
                String to = entry.getPhone();
                if (isDevProfile() && useDevNumber && devTarget != null && !devTarget.isBlank()) {
                    to = devTarget;
                }
                response = smsService.sendSms(entry.getAccount().getId(), to, message);
            } else {
                response.replace("success", false);
                response.put("message", "Phone number is missing or invalid; cannot send notification");
            }
        } catch (ApiException e) {
            logger.error("Failed to send SMS notification: {}", e.getMessage());
            response.replace("success", false);
            response.put("message", "Failed to send SMS notification: " + e.getMessage());
        }
        return response;
    }

    private boolean isValidUsNumber(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() == 11 && digits.startsWith("1")) {
            digits = digits.substring(1);
        }
        return digits.matches("^[2-9]\\d{2}[2-9]\\d{6}$");
    }

    private boolean isDevProfile() {
        if (environment == null) return false;
        for (String profile : environment.getActiveProfiles()) {
            if ("dev".equals(profile)) {
                return true;
            }
        }
        return false;
    }
}
