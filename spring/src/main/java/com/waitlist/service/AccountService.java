package com.waitlist.service;

import com.waitlist.exception.ResourceNotFoundException;
import com.waitlist.model.Account;
import com.waitlist.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.KeyStore.Entry;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.waitlist.dto.ServiceHoursDTO;
import com.waitlist.model.ServiceHours;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private SmsService smsService;

    // entry service is needed so we can clear the queue when the waitlist closes
    @Autowired
    private EntryService entryService;

    @Autowired
    private com.waitlist.controller.EntryController entryController;

    // clock allows deterministic testing
    private Clock clock = Clock.systemDefaultZone();

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public Account getAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public Account createAccount(Account account) {
        return accountRepository.save(account);
    }

    public Account updateAccount(Long id, Account accountDetails) {
        Account account = getAccountById(id);
        account.setAccountName(accountDetails.getAccountName());
        account.setBrandingColorCode(accountDetails.getBrandingColorCode());
        // if the payload contains Twilio credentials we mirror those as well
        if (accountDetails.getTwilioAccountSid() != null) {
            account.setTwilioAccountSid(accountDetails.getTwilioAccountSid());
        }
        if (accountDetails.getTwilioAuthToken() != null) {
            account.setTwilioAuthToken(accountDetails.getTwilioAuthToken());
        }
        return accountRepository.save(account);
    }

    public void deleteAccount(Long id) {
        Account account = getAccountById(id);
        accountRepository.delete(account);
    }

    public Account updateBrandingColor(Long id, String brandingColorCode) {
        Account account = getAccountById(id);
        account.setBrandingColorCode(brandingColorCode);
        return accountRepository.save(account);
    }

    /**
     * Partially updates waitlist configuration; fields are only written
     * when the corresponding value in the request object is non-null.
     */
    public Account updateWaitlistSettings(Long id, com.waitlist.dto.WaitlistSettingsRequest request) {
        Account account = getAccountById(id);
        if (request.getWaitlistEnabled() != null) {
            boolean wasEnabled = account.isWaitlistEnabled();
            account.setWaitlistEnabled(request.getWaitlistEnabled());
            // if the flag is being turned off, immediately deactivate all entries
            if (wasEnabled && Boolean.FALSE.equals(request.getWaitlistEnabled())) {
                entryService.deactivateAllEntries();
                // also notify any SSE listeners that waitlist is disabled
                entryController.emitWaitlistDisabled();
            }
        }
        if (request.getWaitlistOpenTime() != null) {
            account.setWaitlistOpenTime(request.getWaitlistOpenTime());
        }
        if (request.getWaitlistCloseTime() != null) {
            account.setWaitlistCloseTime(request.getWaitlistCloseTime());
        }
        // server-side validation: if enabling waitlist and explicit times exist,
        // ensure current time falls between them.
        if (request.getWaitlistEnabled() != null && request.getWaitlistEnabled()) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now(clock);
            java.time.LocalDateTime open = account.getWaitlistOpenTime();
            java.time.LocalDateTime close = account.getWaitlistCloseTime();
            if (open != null && close != null) {
                if (now.isBefore(open) || now.isAfter(close)) {
                    throw new IllegalArgumentException("Cannot enable waitlist: current time is outside the specified open/close window");
                }
            }
        }
        // apply business hours if provided
        if (request.getServiceHours() != null) {
            Map<String, ServiceHoursDTO> dtoMap = request.getServiceHours();
            // if account has no map yet, create one
            if (account.getServiceHours() == null) {
                account.setServiceHours(new java.util.HashMap<>());
            }
            dtoMap.forEach((key, dto) -> {
                try {
                    DayOfWeek dow = DayOfWeek.valueOf(key.toUpperCase());
                    ServiceHours bh = new ServiceHours();
                    bh.setOpenTime(dto.getOpenTime());
                    bh.setCloseTime(dto.getCloseTime());
                    account.getServiceHours().put(dow, bh);
                } catch (IllegalArgumentException ignored) {
                    // invalid day name; ignore
                }
            });
        }
        Account saved = accountRepository.save(account);
        // if after applying the changes the waitlist is considered closed due to time
        // (open flag is true, but current time is past close) then also clear entries.
        if (!isWaitlistOpen(saved.getId())) {
            entryService.deactivateAllEntries();
            entryController.emitWaitlistDisabled();
        }
        return saved;
    }

    /**
     * Update only the Twilio credential settings for an account.
     */
    public Account updateTwilioSettings(Long id, com.waitlist.dto.TwilioSettingsRequest request) {
        Account account = getAccountById(id);
        boolean updated = false;
        String newSid = null;
        String newToken = null;
        if (request.getAccountSid() != null) {
            newSid = request.getAccountSid();
            updated = true;
        }
        if (request.getAuthToken() != null) {
            newToken = request.getAuthToken();
            updated = true;
        }
        // if credentials are being updated, validate them *before* persisting
        if (updated) {
            String sidToTest = newSid != null ? newSid : account.getTwilioAccountSid();
            String tokenToTest = newToken != null ? newToken : account.getRawTwilioAuthToken();
            try {
                smsService.testCredentials(sidToTest, tokenToTest);
            } catch (Exception ex) {
                // wrap and propagate custom message so caller can surface it
                throw new IllegalArgumentException("Twilio credentials were not saved. Check the credentials and try again.", ex);
            }
        }
        if (newSid != null) {
            account.setTwilioAccountSid(newSid);
        }
        if (newToken != null) {
            account.setTwilioAuthToken(newToken);
        }
        if (request.getSmsEnabled() != null) {
            account.setSmsEnabled(request.getSmsEnabled());
        }
        return accountRepository.save(account);
    }

    /**
     * Retrieve all message templates for the given account.  An empty map will
     * be returned if none have been configured.
     */
    public java.util.Map<String, String> getMessageTemplates(Long id) {
        Account acct = getAccountById(id);
        java.util.Map<String, String> msgs = acct.getMessages();
        return msgs != null ? msgs : new java.util.HashMap<>();
    }

    /**
     * Update the entire set of message templates for an account.
     * Existing templates are replaced by the provided map.
     */
    public Account updateMessageTemplates(Long id, java.util.Map<String, String> templates) {
        Account acct = getAccountById(id);
        acct.setMessages(templates != null ? new java.util.HashMap<>(templates) : new java.util.HashMap<>());
        return accountRepository.save(acct);
    }

    /**
     * Helper that retrieves a single template by key.  Returns null if the key
     * is not configured.
     */
    public String getMessageTemplate(Long accountId, String key) {
        java.util.Map<String, String> msgs = getMessageTemplates(accountId);
        return msgs.get(key);
    }

    /**
     * Convenience helper that determines whether the waitlist is considered open
     * at the current moment.  If the account has never defined open/close times
     * the presence of the enabled flag is used alone.
     */
    public boolean isWaitlistOpen(Long accountId) {
        Account account = getAccountById(accountId);
        boolean open;
        if (!account.isWaitlistEnabled()) {
            open = false;
        } else {
            LocalDateTime now = LocalDateTime.now(clock);
            if (account.getWaitlistOpenTime() != null && account.getWaitlistCloseTime() != null) {
                open = !now.isBefore(account.getWaitlistOpenTime()) && !now.isAfter(account.getWaitlistCloseTime());
            } else {
                // no explicit date/time range; fall back to service hours for today
                DayOfWeek today = now.getDayOfWeek();
                ServiceHours bh = null;
                if (account.getServiceHours() != null) {
                    bh = account.getServiceHours().get(today);
                }
                if (bh != null && bh.getOpenTime() != null && bh.getCloseTime() != null) {
                    LocalTime nowTime = LocalTime.now(clock);
                    open = !nowTime.isBefore(bh.getOpenTime()) && !nowTime.isAfter(bh.getCloseTime());
                } else {
                    open = true;
                }
            }
        }
        if (!open) {
            entryService.deactivateAllEntries();
        }
        return open;
    }

    /**
     * Retrieve the current estimated wait time in minutes (delegates to EntryService).
     * @param accountId 
     */
    public int getEstimatedWaitMinutes(Long accountId) {
        return entryService.calculateEstimatedWaitMinutes(entryService.getAllActiveEntriesForAccount(accountId));
    }

    public Account updateCode(Long id, String code) {
        Account account = getAccountById(id);

        if (account.getCode().equals(code)) {
            throw new IllegalArgumentException("Wrong code provided, try again.");
        }

        account.setCode(null);
        account.ensureCode();
        return accountRepository.save(account);
    }

    public Account getAccountByCode(String code) {
        return accountRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with code: " + code));
    }
}
