package com.waitlist.service;

import com.waitlist.model.Entry;
import com.waitlist.repository.EntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EntryServiceTest {

    @Mock
    private EntryRepository entryRepository;


    @Mock
    private SmsService smsService;

    @Mock
    private org.springframework.core.env.Environment environment;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private EntryService entryService;

    @org.junit.jupiter.api.BeforeEach
    public void initFrontendUrl() {
        try {
            java.lang.reflect.Field f = EntryService.class.getDeclaredField("frontendUrl");
            f.setAccessible(true);
            f.set(entryService, "http://localhost:5173");
        } catch (Exception ignored) {
        }
    }

    @Test
    public void getAllActiveEntriesForAccount_returnsRepositoryList() {
        Entry a = new Entry("A","111",1);
        a.setTimestamp(LocalDateTime.now().minusMinutes(5));
        Entry b = new Entry("B","222",2);
        b.setTimestamp(LocalDateTime.now());
        when(entryRepository.findAllByAccountIdAndActiveTrue(eq(1L))).thenReturn(Arrays.asList(a,b));

        List<Entry> result = entryService.getAllActiveEntriesForAccount(1L);
        assertEquals(2, result.size());
        assertSame(a, result.get(0));
    }

    @Test
    public void create_shouldClearId() {
        Entry in = new Entry("X","000",3);
        when(accountService.isWaitlistOpen(anyLong())).thenReturn(true);
        com.waitlist.model.Account acctObj = new com.waitlist.model.Account("foo", null);
        acctObj.setId(1L);
        acctObj.setCode("acctcode");
        acctObj.setSmsEnabled(true);
        acctObj.setTwilioAccountSid("SID");
        acctObj.setTwilioAuthToken("TOKEN");
        when(accountService.getAccountByCode(anyString())).thenReturn(acctObj);
        when(entryRepository.save(any())).thenAnswer(inv -> {
            Entry e = inv.getArgument(0);
            // simulate JPA assigning ID
            try {
                java.lang.reflect.Field f = Entry.class.getDeclaredField("id");
                f.setAccessible(true);
                f.set(e, 100L);
            } catch (Exception ignored) {}
            return e;
        });
        // environment mock should return empty profiles to avoid NPE in isDevProfile
        when(environment.getActiveProfiles()).thenReturn(new String[0]);
        // account service should return no custom template by default
        when(accountService.getMessageTemplate(eq(1L), eq(MessageTemplate.NEW_ENTRY.getKey())))
                .thenReturn(null);

        // set phone number so sms notification will be sent
        in.setPhone("1234567890");

        Entry out = entryService.create("acct", in);
        // service should return object with generated id and code
        assertNotNull(out.getId());
        assertNotNull(out.getCode());
        assertEquals("X", out.getName());
        // verify sms sent instead of email (account id 1 used)
        verify(smsService).sendSms(eq(1L), eq("1234567890"), contains("Thank you for joining"));
    }

    @Test
    public void create_throwsWhenWaitlistDisabled() {
        Entry in = new Entry("Y","111",1);
        // account service reports closed
        when(accountService.isWaitlistOpen(anyLong())).thenReturn(false);
        // stub code lookup leniently to prevent NPE; return dummy account
        com.waitlist.model.Account acctObj2 = new com.waitlist.model.Account("foo", null);
        acctObj2.setId(1L);
        acctObj2.setCode("acctcode");
        lenient().when(accountService.getAccountByCode(anyString())).thenReturn(acctObj2);
        // environment not needed for this test

        assertThrows(com.waitlist.exception.WaitlistDisabledException.class,
                () -> entryService.create("acct", in));
        // ensure repository never called
        verifyNoInteractions(entryRepository);
    }

    @Test
    public void create_respectsCustomTemplate() {
        Entry in = new Entry("Custom","000",2);
        when(accountService.isWaitlistOpen(anyLong())).thenReturn(true);
        com.waitlist.model.Account acctObj = new com.waitlist.model.Account("foo", null);
        acctObj.setId(1L);
        acctObj.setCode("acctcode");
        acctObj.setSmsEnabled(true);
        acctObj.setTwilioAccountSid("SID");
        acctObj.setTwilioAuthToken("TOKEN");
        when(accountService.getAccountByCode(anyString())).thenReturn(acctObj);
        when(entryRepository.save(any())).thenAnswer(inv -> {
            Entry e = inv.getArgument(0);
            try {
                java.lang.reflect.Field f = Entry.class.getDeclaredField("id");
                f.setAccessible(true);
                f.set(e, 123L);
            } catch (Exception ignored) {}
            return e;
        });
        when(environment.getActiveProfiles()).thenReturn(new String[0]);
        // supply a simple template; first argument will be name only
        when(accountService.getMessageTemplate(eq(1L), eq(MessageTemplate.NEW_ENTRY.getKey())))
                .thenReturn("Hi %s, welcome!");
        in.setPhone("555");

        Entry out = entryService.create("acct", in);
        verify(smsService).sendSms(eq(1L), eq("555"), eq("Hi Custom, welcome!"));
    }
    @Test
    public void getById_notFound() {
        when(entryRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> entryService.getById(5L));
    }

    @Test
    public void resolve_byCode_notFound() {
        when(entryRepository.findByCodeAndActiveTrue("nope")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> entryService.deleteEntry("nope"));
    }


    @Test
    public void deleteEntry_softDeletes() {
        Entry e = new Entry("Z","999",1);
        e.setCode("code20");
        // service currently treats any input as a code lookup, so stub only code path
        lenient().when(entryRepository.findByCodeAndActiveTrue(anyString())).thenReturn(Optional.of(e));
        when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // numeric string will also go through the "code" branch
        Entry out1 = entryService.deleteEntry("20");
        assertFalse(out1.isActive());
        verify(entryRepository).save(e);

        // string code form
        e.setActive(true);
        Entry out2 = entryService.deleteEntry("code20");
        assertFalse(out2.isActive());
    }

    @Test
    public void markCalled_updatesFlag() {
        Entry e = new Entry("Q","000",3);
        e.setCode("code30");
        e.setCalled(false);
        // service resolves via code only
        lenient().when(entryRepository.findByCodeAndActiveTrue(anyString())).thenReturn(Optional.of(e));
        when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Entry out1 = entryService.markCalled("30", true);
        assertTrue(out1.isCalled());

        e.setCalled(false);
        Entry out2 = entryService.markCalled("code30", true);
        assertTrue(out2.isCalled());
    }

    @Test
    public void deactivateAllEntries_invokesRepository() {
        // entryRepository.deactivateAll() has no return value; just ensure it's called
        entryService.deactivateAllEntries();
        verify(entryRepository).deactivateAll();
    }

    @Test
    public void calculateEstimatedWait_correctlyTotals() {
        Entry small = new Entry("S","1",2);
        Entry large = new Entry("L","2",5);
        // no repository interaction is required for this helper
        int result = entryService.calculateEstimatedWaitMinutes(Arrays.asList(small, large));
        // small ->5, large->10
        assertEquals(15, result);
    }

    @Test
    public void sendSmsToEntry_usesSmsService_whenPhoneExists() {
        Entry e = new Entry("M","12345",1);
        e.setCode("c40");
        // ensure account present for sendSms logic
        com.waitlist.model.Account acct = new com.waitlist.model.Account("foo", null);
        acct.setId(1L);
        acct.setCode("acctcode");
        e.setAccount(acct);
        // the service no longer differentiates numeric IDs so only code lookup is used
        lenient().when(entryRepository.findByCodeAndActiveTrue(anyString())).thenReturn(Optional.of(e));
        when(accountService.getAccountByCode(anyString())).thenReturn(acct);
        when(environment.getActiveProfiles()).thenReturn(new String[]{});

        // only code path exercised now
        entryService.sendSmsToEntry("c40", "Another");
        verify(smsService).sendSms(eq(1L), eq("12345"), eq("Another"));
    }

    @Test
    public void sendSmsToEntry_noop_whenNoPhone() {
        Entry e = new Entry("M","",1);
        e.setCode("c41");
        lenient().when(entryRepository.findById(anyLong())).thenReturn(Optional.of(e));
        lenient().when(entryRepository.findByCodeAndActiveTrue(anyString())).thenReturn(Optional.of(e));

        entryService.sendSmsToEntry("41", "Ignored");
        entryService.sendSmsToEntry("c41", "Ignored");
        verifyNoInteractions(smsService);
    }

    @Test
    public void sendSmsToEntry_devProfile_usesFixedNumber() {
        Entry e = new Entry("N","99999",2);
        e.setCode("c50");
        com.waitlist.model.Account acct = new com.waitlist.model.Account("foo", null);
        acct.setId(1L);
        acct.setCode("acctcode");
        e.setAccount(acct);
        // only code lookups are used in resolve
        lenient().when(entryRepository.findByCodeAndActiveTrue(anyString())).thenReturn(Optional.of(e));
        when(accountService.getAccountByCode(anyString())).thenReturn(acct);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        // simulate value injection via reflection rather than environment property
        try {
            java.lang.reflect.Field fDev = EntryService.class.getDeclaredField("devTarget");
            fDev.setAccessible(true);
            fDev.set(entryService, "+18777532491");
            java.lang.reflect.Field fFlag = EntryService.class.getDeclaredField("useDevNumber");
            fFlag.setAccessible(true);
            fFlag.setBoolean(entryService, true);
        } catch (Exception ignored) {}

        entryService.sendSmsToEntry("c50", "Hello");
        verify(smsService, times(1)).sendSms(eq(1L), eq("+18777532491"), eq("Hello"));
    }

    @Test
    public void sendSmsToEntry_devProfile_flagOff_usesNormalNumber() {
        Entry e = new Entry("O","55555",3);
        e.setCode("c51");
        com.waitlist.model.Account acct2 = new com.waitlist.model.Account("foo", null);
        acct2.setId(1L);
        acct2.setCode("acctcode");
        e.setAccount(acct2);
        // only code lookup needed
        lenient().when(entryRepository.findByCodeAndActiveTrue(anyString())).thenReturn(Optional.of(e));
        when(accountService.getAccountByCode(anyString())).thenReturn(acct2);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        try {
            java.lang.reflect.Field fFlag = EntryService.class.getDeclaredField("useDevNumber");
            fFlag.setAccessible(true);
            fFlag.setBoolean(entryService, false);
            java.lang.reflect.Field fDev = EntryService.class.getDeclaredField("devTarget");
            fDev.setAccessible(true);
            fDev.set(entryService, "+18777532491");
        } catch (Exception ignored) {}

        entryService.sendSmsToEntry("c51", "Yo");
        verify(smsService, times(1)).sendSms(eq(1L), eq("55555"), eq("Yo"));
    }
}
