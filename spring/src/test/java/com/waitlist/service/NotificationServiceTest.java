package com.waitlist.service;

import com.waitlist.model.Entry;
import com.waitlist.model.EntrySource;
import com.waitlist.model.Account;
import com.waitlist.model.EventName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private SmsService smsService;

    @Mock
    private FirebaseService firebaseService;

    @Mock
    private AccountService accountService;

    @Mock
    private Environment environment;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    public void notifyOfTableSms_usesFirebaseWhenTokenPresent() {
        Entry e = new Entry("F", "333", 2);
        e.setCode("c72");
        e.setSource(EntrySource.ANDROID_FLUTTER);
        e.setFirebaseAccessToken("token123");

        Account acct = new Account("foo", null);
        acct.setId(1L);
        e.setAccount(acct);

        when(accountService.getMessageTemplate(eq(1L), eq(MessageTemplate.TABLE_READY.getKey()))).thenReturn(null);
        when(firebaseService.sendFirebaseNotification(eq(EventName.NOTIFIED_ENTRY), eq("token123"), anyString())).thenReturn(true);

        notificationService.sendFormattedNotification(e, EventName.NOTIFIED_ENTRY);

        verify(firebaseService).sendFirebaseNotification(eq(EventName.NOTIFIED_ENTRY), eq("token123"), contains("Your table is ready"));
        verifyNoInteractions(smsService);
    }

    @Test
    public void notifyOfTableSms_fallsBackToSms_whenFirebaseFails() {
        Entry e = new Entry("G", "5552345678", 3);
        e.setCode("c73");
        e.setSource(EntrySource.ANDROID_FLUTTER);
        e.setFirebaseAccessToken("token456");
        assertEquals(EntrySource.ANDROID_FLUTTER, e.getSource());

        Account acct = new Account("foo", null);
        acct.setId(1L);
        e.setAccount(acct);

        acct.setSmsEnabled(true);
        when(accountService.getMessageTemplate(eq(1L), eq(MessageTemplate.TABLE_READY.getKey()))).thenReturn(null);
        when(environment.getActiveProfiles()).thenReturn(new String[0]);
        when(firebaseService.sendFirebaseNotification(eq(EventName.NOTIFIED_ENTRY), eq("token456"), anyString())).thenReturn(false);

        notificationService.sendFormattedNotification(e, EventName.NOTIFIED_ENTRY);

        verify(firebaseService).sendFirebaseNotification(eq(EventName.NOTIFIED_ENTRY), eq("token456"), contains("Your table is ready"));
        verify(smsService).sendSms(eq(1L), anyString(), contains("Your table is ready"));
    }

    @Test
    public void sendTableReadyNotification_webSendsSms_firebaseForNonWeb() {
        Entry webEntry = new Entry("W", "5552345678", 2);
        webEntry.setCode("web1");
        webEntry.setSource(EntrySource.WEB);
        Account webAcct = new Account("foo", null);
        webAcct.setId(1L);
        webAcct.setSmsEnabled(true);
        webEntry.setAccount(webAcct);
        when(environment.getActiveProfiles()).thenReturn(new String[0]);

        when(accountService.getMessageTemplate(eq(1L), eq(MessageTemplate.TABLE_READY.getKey()))).thenReturn(null);
        notificationService.sendFormattedNotification(webEntry, EventName.NOTIFIED_ENTRY);
        verify(smsService).sendSms(eq(1L), anyString(), contains("Your table is ready"));

        Entry mobileEntry = new Entry("M", "5551234567", 2);
        mobileEntry.setCode("mob1");
        mobileEntry.setSource(EntrySource.ANDROID_FLUTTER);
        mobileEntry.setFirebaseAccessToken("tok123");
        mobileEntry.setAccount(webAcct);

        when(firebaseService.sendFirebaseNotification(eq(EventName.NOTIFIED_ENTRY), eq("tok123"), anyString())).thenReturn(true);

        notificationService.sendFormattedNotification(mobileEntry, EventName.NOTIFIED_ENTRY);
        verify(firebaseService).sendFirebaseNotification(eq(EventName.NOTIFIED_ENTRY), eq("tok123"), contains("Your table is ready"));
    }

    @Test
    public void sendSmsNotification_callsSmsService_directly_whenEnabledAndValidPhone() {
        Entry e = new Entry("Test", "5552345678", 2);
        Account acct = new Account("foo", null);
        acct.setId(1L);
        acct.setSmsEnabled(true);
        e.setAccount(acct);

        when(environment.getActiveProfiles()).thenReturn(new String[0]);

        notificationService.sendSmsNotification(e, "test");

        verify(smsService).sendSms(eq(1L), eq("5552345678"), contains("test"));
    }
}

