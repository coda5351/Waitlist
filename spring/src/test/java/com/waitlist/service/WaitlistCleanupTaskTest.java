package com.waitlist.service;

import com.waitlist.model.Account;
import com.waitlist.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WaitlistCleanupTaskTest {

    @Mock
    private AccountService accountService;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private WaitlistCleanupTask cleanupTask;

    @Test
    public void runCleanup_callsAccountServiceForEachAccount() {
        Account a1 = new Account();
        a1.setId(1L);
        Account a2 = new Account();
        a2.setId(2L);
        when(accountRepository.findAll()).thenReturn(Arrays.asList(a1, a2));

        cleanupTask.runCleanup();

        verify(accountService).isWaitlistOpen(1L);
        verify(accountService).isWaitlistOpen(2L);
    }
}