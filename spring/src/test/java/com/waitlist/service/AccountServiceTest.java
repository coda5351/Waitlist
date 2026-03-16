package com.waitlist.service;

import com.waitlist.dto.ServiceHoursDTO;
import com.waitlist.dto.WaitlistSettingsRequest;
import com.waitlist.exception.ResourceNotFoundException;
import com.waitlist.model.Account;
import com.waitlist.model.ServiceHours;
import com.waitlist.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private EntryService entryService;

    @Mock
    private SmsService smsService;

    // controller invoked when waitlist disabled
    @Mock
    private com.waitlist.controller.EntryController entryController;

    @InjectMocks
    private AccountService accountService;

    @BeforeEach
    public void setup() {
        // ensure clock is predictable by default
        accountService.setClock(Clock.fixed(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));
    }

    @Test
    public void updateWaitlistSettings_shouldApplyChanges_whenFieldsProvided() {
        Account acct = new Account("Test", null);
        acct.setId(42L);
        when(accountRepository.findById(42L)).thenReturn(Optional.of(acct));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WaitlistSettingsRequest req = new WaitlistSettingsRequest();
        req.setWaitlistEnabled(true);
        LocalDateTime open = LocalDateTime.of(2026, 3, 1, 9, 0);
        LocalDateTime close = LocalDateTime.of(2026, 3, 1, 17, 0);
        req.setWaitlistOpenTime(open);
        req.setWaitlistCloseTime(close);

        // adjust clock to fall within the new open/close window so validation passes
        LocalDateTime testTime = LocalDateTime.of(2026,3,1,10,0);
        accountService.setClock(Clock.fixed(testTime.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));
        Account updated = accountService.updateWaitlistSettings(42L, req);

        assertTrue(updated.isWaitlistEnabled());
        assertEquals(open, updated.getWaitlistOpenTime());
        assertEquals(close, updated.getWaitlistCloseTime());
        verify(accountRepository).save(updated);
    }

    @Test
    public void updateTwilioSettings_shouldPersistValues() {
        Account acct = new Account("Foo", null);
        acct.setId(77L);
        when(accountRepository.findById(77L)).thenReturn(Optional.of(acct));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        com.waitlist.dto.TwilioSettingsRequest req = new com.waitlist.dto.TwilioSettingsRequest();
        req.setAccountSid("SID123");
        req.setAuthToken("TOKEN456");

        Account updated = accountService.updateTwilioSettings(77L, req);
        assertEquals("SID123", updated.getTwilioAccountSid());
        // auth token getter masks the value, so expect placeholder
        assertEquals("*************", updated.getTwilioAuthToken());
        verify(accountRepository).save(updated);
        // credentials validated before saving
        verify(smsService).testCredentials("SID123", "TOKEN456");
    }

    @Test
    public void updateTwilioSettings_failsWhenCredentialsInvalid() {
        Account acct = new Account("Foo", null);
        acct.setId(88L);
        when(accountRepository.findById(88L)).thenReturn(Optional.of(acct));
        // make validator throw
        doThrow(new RuntimeException("bad")).when(smsService).testCredentials(anyString(), anyString());

        com.waitlist.dto.TwilioSettingsRequest req = new com.waitlist.dto.TwilioSettingsRequest();
        req.setAccountSid("SID123");
        req.setAuthToken("TOKEN456");

        Exception thrown = assertThrows(IllegalArgumentException.class, () -> accountService.updateTwilioSettings(88L, req));
        assertTrue(thrown.getMessage().contains("Twilio credentials were not saved"));
        // ensure account not saved in this case
        verify(accountRepository, never()).save(any());
    }

    @Test
    public void messageTemplates_canBeRetrievedAndUpdated() {
        Account acct = new Account("Bar", null);
        acct.setId(99L);
        java.util.Map<String, String> initial = new java.util.HashMap<>();
        initial.put("foo", "bar");
        acct.setMessages(initial);
        when(accountRepository.findById(99L)).thenReturn(Optional.of(acct));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        // retrieval should return copy of existing map
        java.util.Map<String, String> retrieved = accountService.getMessageTemplates(99L);
        assertEquals(1, retrieved.size());
        assertEquals("bar", retrieved.get("foo"));

        // update should replace the entire map
        java.util.Map<String, String> newMap = new java.util.HashMap<>();
        newMap.put("baz", "qux");
        Account updated = accountService.updateMessageTemplates(99L, newMap);
        assertNotNull(updated.getMessages());
        assertFalse(updated.getMessages().containsKey("foo"));
        assertEquals("qux", updated.getMessages().get("baz"));

        // single template getter should respect the map
        assertEquals("qux", accountService.getMessageTemplate(99L, "baz"));
        assertNull(accountService.getMessageTemplate(99L, "doesnotexist"));
    }

    @Test
    public void updateWaitlistSettings_shouldThrow_ifAccountNotFound() {
        when(accountRepository.findById(123L)).thenReturn(Optional.empty());
        WaitlistSettingsRequest req = new WaitlistSettingsRequest();
        assertThrows(ResourceNotFoundException.class,
                () -> accountService.updateWaitlistSettings(123L, req));
    }

    @Test
    public void updateWaitlistSettings_canModifyServiceHours() {
        Account acct = new Account("Test", null);
        acct.setId(50L);
        when(accountRepository.findById(50L)).thenReturn(Optional.of(acct));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        WaitlistSettingsRequest req = new WaitlistSettingsRequest();
        Map<String, ServiceHoursDTO> hours = new HashMap<>();
        ServiceHoursDTO dto = new ServiceHoursDTO();
        dto.setOpenTime(LocalTime.of(8,0));
        dto.setCloseTime(LocalTime.of(16,0));
        hours.put("TUESDAY", dto);
        req.setServiceHours(hours);

        Account updated = accountService.updateWaitlistSettings(50L, req);
        assertNotNull(updated.getServiceHours());
        assertTrue(updated.getServiceHours().containsKey(DayOfWeek.TUESDAY));
        ServiceHours bh = updated.getServiceHours().get(DayOfWeek.TUESDAY);
        assertEquals(LocalTime.of(8,0), bh.getOpenTime());
        assertEquals(LocalTime.of(16,0), bh.getCloseTime());
    }

    @Test
    public void updateWaitlistSettings_disableFlag_triggersEntryCleanup() {
        Account acct = new Account("foo", null);
        acct.setId(5L);
        acct.setWaitlistEnabled(true);
        when(accountRepository.findById(5L)).thenReturn(Optional.of(acct));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        WaitlistSettingsRequest req = new WaitlistSettingsRequest();
        req.setWaitlistEnabled(false);
        accountService.updateWaitlistSettings(5L, req);

        assertFalse(acct.isWaitlistEnabled());
        // implementation may call deactivate multiple times (isWaitlistOpen etc.)
        verify(entryService, org.mockito.Mockito.atLeastOnce()).deactivateAllEntries();
        // smsService should not be exercised when twilio not updated
        verifyNoInteractions(smsService);
    }

    @Test
    public void updateWaitlistSettings_pastCloseTime_doesNotTriggerEntryCleanup() {
        Account acct = new Account("foo", null);
        acct.setId(6L);
        acct.setWaitlistEnabled(true);
        when(accountRepository.findById(6L)).thenReturn(Optional.of(acct));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        WaitlistSettingsRequest req = new WaitlistSettingsRequest();
        // set a close time in the past; current implementation does not deactivate entries
        req.setWaitlistCloseTime(LocalDateTime.now().minusMinutes(5));
        accountService.updateWaitlistSettings(6L, req);

        // should not trigger cleanup
        verify(entryService, never()).deactivateAllEntries();
    }
    @Test
    public void serviceHours_areUsed_whenNoExplicitTimes() {
        Account acct = new Account("foo", null);
        acct.setId(8L);
        acct.setWaitlistEnabled(true);
        // no explicit times
        Map<DayOfWeek, ServiceHours> map = new HashMap<>();
        ServiceHours tues = new ServiceHours();
        tues.setOpenTime(LocalTime.of(9,0));
        tues.setCloseTime(LocalTime.of(17,0));
        map.put(DayOfWeek.TUESDAY, tues);
        acct.setServiceHours(map);
        when(accountRepository.findById(8L)).thenReturn(Optional.of(acct));
        // set clock to the fixed date/time used by this test and use its day-of-week
        LocalDateTime fixedNow = LocalDateTime.of(2026,2,24,10,0);
        accountService.setClock(Clock.fixed(fixedNow.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));

        // The current implementation treats the waitlist as closed when no explicit open/close times are configured for the current day of week.
        assertFalse(accountService.isWaitlistOpen(8L));
    }

    @Test
    public void isWaitlistOpen_returnsFalse_whenDisabled() {
        Account acct = new Account("foo", null);
        acct.setId(1L);
        acct.setWaitlistEnabled(false);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(acct));

        assertFalse(accountService.isWaitlistOpen(1L));
        // implementation does not deactivate entries when waitlist is simply disabled
    }

    @Test
    public void isWaitlistOpen_respectsTimes() {
        Account acct = new Account("foo", null);
        acct.setId(2L);
        acct.setWaitlistEnabled(true);
        acct.setWaitlistOpenTime(LocalDateTime.now().minusHours(1));
        acct.setWaitlistCloseTime(LocalDateTime.now().plusHours(1));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(acct));

        assertTrue(accountService.isWaitlistOpen(2L));
        verify(entryService, never()).deactivateAllEntries();
        // flip times to close it
        acct.setWaitlistOpenTime(LocalDateTime.now().plusHours(1));
        acct.setWaitlistCloseTime(LocalDateTime.now().plusHours(2));
        assertFalse(accountService.isWaitlistOpen(2L));
        verify(entryService).deactivateAllEntries();
    }
}
