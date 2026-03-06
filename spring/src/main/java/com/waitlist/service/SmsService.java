package com.waitlist.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import com.waitlist.model.Account;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SmsService.class);

    private final AccountService accountService;

    // the "from" number remains configurable via properties
    @Value("${twilio.from-number:}")
    private String fromNumber;

    // Messaging service SID used for send-test-message calls.  Read from
    // configuration so it can be replaced in environments where the magic
    // default is not valid.  Kept separate from the normal "from" number
    // because Twilio requires the SID for messaging service usage.
    @Value("${twilio.test-message-sid:}")
    private String twilioTestMessageSid;    

    public SmsService(@Lazy AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Sends a message using credentials stored on the given account.
     */
    public void sendSms(Long accountId, String to, String body) {
        Account acct = accountService.getAccountById(accountId);
        if (!acct.isSmsEnabled()) {
            // do not call external API when disabled; just log for diagnostic purposes
            logger.info("SMS disabled for account {} - would have sent to {} with body: {}", accountId, to, body);
            return;
        }

        String accountSid = acct.getTwilioAccountSid();
        // use raw accessor to retrieve real token (masked getter is for JSON only)
        String authToken = acct.getRawTwilioAuthToken();
        if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()) {
            throw new IllegalStateException("Twilio credentials are not configured for account " + accountId);
        } else {
            Twilio.init(accountSid, authToken);
            Message.creator(new PhoneNumber(to), new PhoneNumber(fromNumber), body).create();
        }
    }

    /**
     * Helper used when validating new credentials prior to saving them.  This
     * does not look up an account and therefore avoids having to persist first.
     */
    public void testCredentials(String accountSid, String authToken) {
        if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()) {
            throw new IllegalArgumentException("credentials must be non-empty");
        }
        Twilio.init(accountSid, authToken);
        // determine 'from' number: use configured value if present (ensures valid account
        // phone is used), otherwise fall back to the magic testing number used prior to
        // introducing the configuration property.
        if (fromNumber != null && !fromNumber.isBlank()) {
            throw new IllegalStateException("from number must be configured to use testCredentials");
        }
        Message.creator(new PhoneNumber(fromNumber), twilioTestMessageSid, "This is a test message.").create();
    }

    /**
     * Convenience overload that uses the primary account (1) for compatibility
     * with existing tests and callers.
     */
    public void sendSms(String to, String body) {
        sendSms(1L, to, body);
    }

    // setter used by tests to override the injected property value
    public void setTwilioTestMessageSid(String twilioTestMessageSid) {
        this.twilioTestMessageSid = twilioTestMessageSid;
    }
}