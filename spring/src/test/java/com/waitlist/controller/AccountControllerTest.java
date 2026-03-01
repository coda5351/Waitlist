package com.waitlist.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waitlist.model.Account;
import com.waitlist.service.AccountService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
public class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    // security beans that are pulled in by configuration
    @MockBean
    private com.waitlist.security.JwtTokenProvider jwtTokenProvider;

    @Test
    public void patchWaitlistSettings_shouldReturnUpdatedAccount() throws Exception {
        Account acct = new Account("foo", null);
        acct.setId(5L);
        acct.setWaitlistEnabled(true);
        acct.setWaitlistOpenTime(LocalDateTime.of(2026,3,1,9,0));
        when(accountService.updateWaitlistSettings(eq(5L), Mockito.any()))
                .thenReturn(acct);

        Map<String,Object> payload = Map.of(
                "waitlistEnabled", true,
                "waitlistOpenTime", "2026-03-01T09:00:00",
                "serviceHours", Map.of("MONDAY", Map.of("openTime","09:00:00","closeTime","17:00:00"))
        );

        mockMvc.perform(patch("/api/accounts/5/waitlist-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waitlistEnabled").value(true));
    }

    @Test
    public void patchTwilioSettings_shouldReturnUpdatedAccount() throws Exception {
        Account acct = new Account("foo", null);
        acct.setId(9L);
        when(accountService.updateTwilioSettings(eq(9L), Mockito.any()))
                .thenReturn(acct);

        Map<String,Object> payload = Map.of(
                "accountSid", "ABC",
                "authToken", "XYZ",
                "smsEnabled", true
        );

        mockMvc.perform(patch("/api/accounts/9/twilio-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9));
    }

    @Test
    public void patchTwilioSettings_shouldUpdateSmsFlagAlone() throws Exception {
        Account acct = new Account("foo", null);
        acct.setId(12L);
        acct.setSmsEnabled(false);
        when(accountService.updateTwilioSettings(eq(12L), Mockito.any()))
                .thenReturn(acct);

        Map<String,Object> payload = Map.of(
                "smsEnabled", false
        );

        mockMvc.perform(patch("/api/accounts/12/twilio-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.smsEnabled").value(false));
    }

    @Test
    public void patchTwilioSettings_shouldReturnErrorMessage_onValidationFailure() throws Exception {
        when(accountService.updateTwilioSettings(eq(10L), Mockito.any()))
                .thenThrow(new IllegalArgumentException("Twilio credentials were not saved. Check the credentials and try again."));

        Map<String,Object> payload = Map.of(
                "accountSid", "bad",
                "authToken", "bad",
                "smsEnabled", false
        );

        mockMvc.perform(patch("/api/accounts/10/twilio-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Twilio credentials were not saved. Check the credentials and try again."));
    }

    @Test
    public void getAccount_shouldMaskAuthTokenInResponse() throws Exception {
        Account acct = new Account("foo", null);
        acct.setId(8L);
        acct.setSmsEnabled(true);
        acct.setTwilioAccountSid("SID123");
        acct.setTwilioAuthToken("secret");
        when(accountService.getAccountById(8L)).thenReturn(acct);

        mockMvc.perform(get("/api/accounts/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.smsEnabled").value(true))
                .andExpect(jsonPath("$.twilioAccountSid").value("SID123"))
                .andExpect(jsonPath("$.twilioAuthToken").value("*************"));
    }

    @Test
    public void patchWaitlistSettings_shouldError_whenOutsideTimes() throws Exception {
        when(accountService.updateWaitlistSettings(eq(11L), Mockito.any()))
                .thenThrow(new IllegalArgumentException("Cannot enable waitlist: current time is outside the specified open/close window"));

        Map<String,Object> payload = Map.of(
                "waitlistEnabled", true,
                "waitlistOpenTime", "2026-03-01T10:00:00",
                "waitlistCloseTime", "2026-03-01T09:00:00"
        );

        mockMvc.perform(patch("/api/accounts/11/waitlist-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot enable waitlist: current time is outside the specified open/close window"));
    }


    @Test
    public void getMessages_shouldReturnMap() throws Exception {
        java.util.Map<String,String> msgs = Map.of("k1","v1","k2","v2");
        when(accountService.getMessageTemplates(3L)).thenReturn(msgs);

        mockMvc.perform(get("/api/accounts/3/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.k1").value("v1"))
                .andExpect(jsonPath("$.k2").value("v2"));
    }

    @Test
    public void patchMessages_shouldUpdateAndReturnNewMap() throws Exception {
        Account acct = new Account("foo", null);
        acct.setId(4L);
        java.util.Map<String,String> updated = Map.of("hello","world");
        acct.setMessages(new java.util.HashMap<>(updated));
        when(accountService.updateMessageTemplates(eq(4L), Mockito.any()))
                .thenReturn(acct);

        mockMvc.perform(patch("/api/accounts/4/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hello").value("world"));
    }
}
