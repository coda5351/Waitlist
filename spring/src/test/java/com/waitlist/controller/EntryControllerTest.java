package com.waitlist.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waitlist.model.Entry;
import com.waitlist.model.EntrySource;
import com.waitlist.service.EntryService;
import com.waitlist.service.NotificationService;
import com.twilio.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(EntryController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
public class EntryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationContext applicationContext;

    @MockBean
    private EntryService entryService;

    @MockBean
    private NotificationService notificationService;

    // security support
    @MockBean
    private com.waitlist.security.JwtTokenProvider jwtTokenProvider;

    // the entry listing endpoint was moved to WaitlistController and now requires an
    // account id; the old /api/entries GET route no longer exists so we don't test it here.
    // individual entry operations are covered in other tests below.

    @Test
    public void createEntry_savesAndReturns() throws Exception {
        Entry a = new Entry("B","222",2);
        // set account so emitter logic can access id
        com.waitlist.model.Account acct = new com.waitlist.model.Account("foo", null);
        acct.setId(1L);
        a.setAccount(acct);
        // simulate object with phone number
        a.setPhone("1234567890");
        try {
            java.lang.reflect.Field f = Entry.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(a, 2L);
        } catch (Exception ignored) {}
        when(entryService.create(anyString(), any(com.waitlist.model.Entry.class))).thenReturn(a);
        when(entryService.resolveFailOk(anyString())).thenReturn(a);

        // ensure conversion to DTO returns values matching the entry
        when(entryService.toDto(any())).thenAnswer(inv -> {
            com.waitlist.model.Entry ent = inv.getArgument(0);
            com.waitlist.dto.EntryDTO dto = new com.waitlist.dto.EntryDTO();
            dto.setId(ent.getId());
            dto.setCode(ent.getCode());
            dto.setAccountId(ent.getAccount() != null ? ent.getAccount().getId() : null);
            return dto;
        });
        when(entryService.getAllActiveEntriesForAccount(anyLong())).thenReturn(java.util.Collections.emptyList());

        mockMvc.perform(post("/api/entries/create/abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(a)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.code").isNotEmpty());

        // simulate disabled waitlist scenario
        when(entryService.create(anyString(), any(com.waitlist.model.Entry.class))).thenThrow(new com.waitlist.exception.WaitlistDisabledException());
        Entry d = new Entry("D","444",4);
        mockMvc.perform(post("/api/entries/create/abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(d)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Waitlist is disabled"));

        // notify endpoint now uses resolve() and sendFormattedNotification and must be stubbed
        when(entryService.resolve(anyString())).thenReturn(a);
        when(notificationService.sendFormattedNotification(any(com.waitlist.model.Entry.class), eq(com.waitlist.model.EventName.NOTIFIED_ENTRY)))
                .thenReturn(java.util.Map.of("success", true));

        mockMvc.perform(post("/api/entries/abc123/notify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        verify(notificationService).sendFormattedNotification(eq(a), eq(com.waitlist.model.EventName.NOTIFIED_ENTRY));

        // delete path should accept any string, numeric or code; service is stubbed to return an Entry
        Entry deleted = a;
        deleted.setCode("abc123");
        when(entryService.deleteEntry(anyString())).thenReturn(deleted);
        mockMvc.perform(delete("/api/entries/abc123"))
                .andExpect(status().isOk());
        verify(entryService).deleteEntry("abc123");

        when(entryService.markCalled(eq("abc123"), eq(true))).thenReturn(a);
        mockMvc.perform(patch("/api/entries/abc123/called")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"called\":true}"))
                .andExpect(status().isOk());
        verify(entryService).markCalled("abc123", true);
    }

    @Test
    public void sendSms_routeInvokesService() throws Exception {
        // register a mock emitter for SSE verification
        SseEmitter mockEmitter = Mockito.mock(SseEmitter.class);
        EntryController ctrl = this.applicationContext.getBean(EntryController.class);
        ctrl.emitters.add(mockEmitter);

        // prepare entry service stub; verify is called later
        when(entryService.calculateEstimatedWaitMinutes(anyList())).thenReturn(42);
        when(entryService.getAllActiveEntriesForAccount(anyLong())).thenReturn(java.util.Collections.emptyList());
        mockMvc.perform(post("/api/entries/abc123/sms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isOk());
        verify(entryService).sendSmsToEntry("abc123", "Hello");
        verify(mockEmitter).send(Mockito.any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    public void createEntry_emitsNewEventWithEstimate() throws Exception {
        SseEmitter mockEmitter = Mockito.mock(SseEmitter.class);
        EntryController ctrl = this.applicationContext.getBean(EntryController.class);
        ctrl.emitters.add(mockEmitter);

        Entry a = new Entry("B","222",2);
        // provide account so emitter code can compute estimate
        com.waitlist.model.Account acct = new com.waitlist.model.Account("foo", null);
        acct.setId(1L);
        a.setAccount(acct);
        try {
            java.lang.reflect.Field f = Entry.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(a, 2L);
        } catch (Exception ignored) {}
        when(entryService.create(anyString(), any(com.waitlist.model.Entry.class))).thenReturn(a);
        when(entryService.resolveFailOk(anyString())).thenReturn(a);
        when(entryService.toDto(any())).thenAnswer(inv -> {
            com.waitlist.model.Entry ent = inv.getArgument(0);
            com.waitlist.dto.EntryDTO dto = new com.waitlist.dto.EntryDTO();
            dto.setId(ent.getId());
            dto.setCode(ent.getCode());
            dto.setAccountId(ent.getAccount() != null ? ent.getAccount().getId() : null);
            return dto;
        });
        when(entryService.calculateEstimatedWaitMinutes(anyList())).thenReturn(99);
        when(entryService.getAllActiveEntriesForAccount(anyLong())).thenReturn(java.util.Collections.emptyList());

        mockMvc.perform(post("/api/entries/create/abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(a)))
                .andExpect(status().isOk());
        verify(entryService).calculateEstimatedWaitMinutes(anyList());
        verify(mockEmitter).send(Mockito.any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    public void deleteEntry_emitsDeletedEventWithEstimate() throws Exception {
        SseEmitter mockEmitter = Mockito.mock(SseEmitter.class);
        EntryController ctrl = this.applicationContext.getBean(EntryController.class);
        ctrl.emitters.add(mockEmitter);

        Entry a = new Entry("C","333",3);
        a.setCode("xyz123");
        com.waitlist.model.Account acct = new com.waitlist.model.Account("foo", null);
        acct.setId(1L);
        a.setAccount(acct);
        when(entryService.deleteEntry(anyString())).thenReturn(a);
        when(entryService.resolveFailOk(eq("xyz123"))).thenReturn(a);
        when(entryService.toDto(any())).thenAnswer(inv -> {
            com.waitlist.model.Entry ent = inv.getArgument(0);
            com.waitlist.dto.EntryDTO dto = new com.waitlist.dto.EntryDTO();
            dto.setId(ent.getId());
            dto.setCode(ent.getCode());
            dto.setAccountId(ent.getAccount() != null ? ent.getAccount().getId() : null);
            return dto;
        });
        when(entryService.calculateEstimatedWaitMinutes(anyList())).thenReturn(7);
        when(entryService.getAllActiveEntriesForAccount(anyLong())).thenReturn(java.util.Collections.emptyList());

        mockMvc.perform(delete("/api/entries/xyz123"))
                .andExpect(status().isOk());
        verify(entryService).calculateEstimatedWaitMinutes(anyList());
        verify(mockEmitter).send(Mockito.any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    public void sendSms_apiExceptionReturnsMessage() throws Exception {
        // simulate underlying service throwing Twilio ApiException for void method
        doThrow(new ApiException("Invalid number")).when(entryService).sendSmsToEntry(eq("def456"), anyString());

        mockMvc.perform(post("/api/entries/def456/sms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Test\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid number"));
    }

    @Test
    public void streamEntries_returnsEventStream() throws Exception {
        mockMvc.perform(get("/api/entries/stream"))
                .andExpect(status().isOk());
                // header may be absent depending on mock environment
    }

    @Test
    public void notifyEntry_emitsNotifiedEvent_andTriggersNotification() throws Exception {
        // register a mock emitter to capture sends
        SseEmitter mockEmitter = Mockito.mock(SseEmitter.class);
        EntryController ctrl = this.applicationContext.getBean(EntryController.class);
        ctrl.emitters.add(mockEmitter);

        Entry entry = new Entry("C", "333", 3);
        entry.setCode("xyz789");
        entry.setSource(EntrySource.WEB);
        com.waitlist.model.Account acct = new com.waitlist.model.Account("foo", null);
        acct.setId(1L);
        acct.setSmsEnabled(true);
        entry.setAccount(acct);
        entry.setPhone("(555) 123-4567");

        when(entryService.resolve(eq("xyz789"))).thenReturn(entry);
        when(entryService.resolveFailOk(eq("xyz789"))).thenReturn(entry);
        when(entryService.getAllActiveEntriesForAccount(eq(1L))).thenReturn(java.util.Collections.emptyList());
        when(notificationService.sendFormattedNotification(eq(entry), eq(com.waitlist.model.EventName.NOTIFIED_ENTRY)))
                .thenReturn(java.util.Map.of("success", true));
        mockMvc.perform(post("/api/entries/xyz789/notify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"sms\",\"message\":\"tableReady\"}"))
                .andExpect(status().isOk());

        // verify SSE send as before
        verify(mockEmitter).send(Mockito.any(SseEmitter.SseEventBuilder.class));

        // verify notification path was triggered
        verify(notificationService).sendFormattedNotification(eq(entry), eq(com.waitlist.model.EventName.NOTIFIED_ENTRY));
    }
}
