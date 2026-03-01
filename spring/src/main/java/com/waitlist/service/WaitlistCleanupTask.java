package com.waitlist.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.waitlist.repository.AccountRepository;

/**
 * Periodically evaluate all accounts to ensure the waitlist deactivation logic
 * has been applied when a list is closed due to time or flag changes.  The
 * actual cleanup is performed by {@link AccountService#isWaitlistOpen(Long)},
 * which will in turn call the entry service if the list is not open.
 */
@Component
public class WaitlistCleanupTask {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    // run once a minute by default; interval can be overridden with
    // spring.property waitlist.cleanup.interval (milliseconds)
    @Scheduled(fixedRateString = "${waitlist.cleanup.interval:60000}")
    public void runCleanup() {
        accountRepository.findAll().forEach(a -> {
            try {
                accountService.isWaitlistOpen(a.getId());
            } catch (Exception ignore) {
                // if account disappears or another problem occurs just continue
            }
        });
    }
}