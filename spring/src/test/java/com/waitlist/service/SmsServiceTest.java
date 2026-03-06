package com.waitlist.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.waitlist.model.Account;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SmsServiceTest {

    @Mock
    private AccountService accountService;

    @InjectMocks
    private SmsService smsService;

    @org.junit.jupiter.api.BeforeEach
    public void init() {
        // ensure the test SID field isn't null when Mockito creates the service
        smsService.setTwilioTestMessageSid("MG809047ae156a31adfad093128ac80b03");
    }

    @Test
    public void sendSms_usesRawToken() {
        Account acct = new Account("foo", null);
        acct.setId(1L);
        acct.setSmsEnabled(true);
        acct.setTwilioAccountSid("sid123");
        acct.setTwilioAuthToken("secret-token");

        when(accountService.getAccountById(1L)).thenReturn(acct);

        try (MockedStatic<Twilio> twilioMock = org.mockito.Mockito.mockStatic(Twilio.class);
             MockedStatic<Message> messageMock = org.mockito.Mockito.mockStatic(Message.class)) {
            MessageCreator creator = org.mockito.Mockito.mock(MessageCreator.class);
            when(creator.create()).thenReturn(null);
            messageMock.when(() -> Message.creator(
                    any(com.twilio.type.PhoneNumber.class),
                    any(com.twilio.type.PhoneNumber.class),
                    any(String.class)))
                .thenReturn(creator);

            smsService.sendSms(1L, "+1000", "hello");
            twilioMock.verify(() -> Twilio.init(eq("sid123"), eq("secret-token")));
        }
    }

    @Test
    public void sendSms_throwsWhenMissingCredentials() {
        Account acct = new Account("foo", null);
        acct.setId(2L);
        acct.setSmsEnabled(true);
        acct.setTwilioAccountSid(null);
        acct.setTwilioAuthToken(null);
        when(accountService.getAccountById(2L)).thenReturn(acct);

        assertThrows(IllegalStateException.class, () -> smsService.sendSms(2L, "+1000", "msg"));
    }

    @Test
    public void sendSms_logsWhenDisabled() {
        Account acct = new Account("foo", null);
        acct.setId(3L);
        acct.setSmsEnabled(false);
        acct.setTwilioAccountSid("sidX");
        acct.setTwilioAuthToken("tokY");
        when(accountService.getAccountById(3L)).thenReturn(acct);

        // since logging is side effect only, just call and verify Twilio.init never invoked
        try (MockedStatic<Twilio> twilioMock = org.mockito.Mockito.mockStatic(Twilio.class);
             MockedStatic<Message> messageMock = org.mockito.Mockito.mockStatic(Message.class)) {
            smsService.sendSms(3L, "+2000", "hi");
            twilioMock.verifyNoInteractions();
            messageMock.verifyNoInteractions();
        }
    }

    @Test
    public void testCredentials_usesConfiguredSid() {
        // configure test SID via setter (mocks aren't involved)
        smsService.setTwilioTestMessageSid("TEST_SID");

        try (MockedStatic<Twilio> twilioMock = org.mockito.Mockito.mockStatic(Twilio.class);
             MockedStatic<Message> messageMock = org.mockito.Mockito.mockStatic(Message.class)) {
            MessageCreator creator = org.mockito.Mockito.mock(MessageCreator.class);
            when(creator.create()).thenReturn(null);
            messageMock.when(() -> Message.creator(
                    any(com.twilio.type.PhoneNumber.class),
                    eq("TEST_SID"),
                    any(String.class)))
                .thenReturn(creator);

            smsService.testCredentials("sid", "token");
        }
    }
}
