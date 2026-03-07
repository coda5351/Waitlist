package com.waitlist.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waitlist.model.Account;
import com.waitlist.service.AccountService;
import com.waitlist.service.EntryService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WaitlistController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
public class WaitlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    @MockBean
    private EntryService entryService;

    // other security beans pulled in by configuration
    @MockBean
    private com.waitlist.security.JwtTokenProvider jwtTokenProvider;

    @Test
    public void getWaitlistStatus_shouldReturnOpenFlag() throws Exception {
        when(accountService.isWaitlistOpen(7L)).thenReturn(true);
        Account acct = new Account("foo", null);
        acct.setId(7L);
        acct.setWaitlistEnabled(true);
        acct.setWaitlistOpenTime(LocalDateTime.of(2026,4,1,8,0));
        acct.setWaitlistCloseTime(LocalDateTime.of(2026,4,1,17,0));
        // add simple business hours entry
        java.util.Map<java.time.DayOfWeek, com.waitlist.model.ServiceHours> bh = new java.util.HashMap<>();
        com.waitlist.model.ServiceHours monday = new com.waitlist.model.ServiceHours();
        monday.setOpenTime(java.time.LocalTime.of(8,0));
        monday.setCloseTime(java.time.LocalTime.of(16,0));
        bh.put(java.time.DayOfWeek.MONDAY, monday);
        acct.setServiceHours(bh);
        acct.setSmsEnabled(true);
        when(accountService.getAccountByCode(eq("7"))).thenReturn(acct);
        when(accountService.getEstimatedWaitMinutes(eq(7L))).thenReturn(25);
        when(entryService.getAllActiveEntryDtosForAccount(eq(7L))).thenReturn(java.util.Collections.emptyList());

        mockMvc.perform(get("/api/waitlists/7/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(true))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.serviceHours.MONDAY.openTime").exists())
                .andExpect(jsonPath("$.estimatedWait").value(25));
    }
}
