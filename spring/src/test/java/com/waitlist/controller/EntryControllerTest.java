package com.waitlist.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waitlist.model.Entry;
import com.waitlist.service.EntryService;
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

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

    // security support
    @MockBean
    private com.waitlist.security.JwtTokenProvider jwtTokenProvider;

    @Test
    public void listEntries_returnsList() throws Exception {
        Entry a = new Entry("A","111",1);
        a.setTimestamp(LocalDateTime.now());
        // set id via reflection to simulate persistent object
        try {
            java.lang.reflect.Field f = Entry.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(a, 1L);
        } catch (Exception ignored) {}
        when(entryService.getAll()).thenReturn(Arrays.asList(a));

        mockMvc.perform(get("/api/entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("A"))
                .andExpect(jsonPath("$[0].code").isNotEmpty());
    }

    @Test
    public void createEntry_savesAndReturns() throws Exception {
        Entry a = new Entry("B","222",2);
        // simulate object with phone number
        a.setPhone("1234567890");
        try {
            java.lang.reflect.Field f = Entry.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(a, 2L);
        } catch (Exception ignored) {}
        when(entryService.create(any())).thenReturn(a);

        mockMvc.perform(post("/api/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(a)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.code").isNotEmpty());

        // simulate disabled waitlist scenario
        when(entryService.create(any())).thenThrow(new com.waitlist.exception.WaitlistDisabledException());
        Entry d = new Entry("D","444",4);
        mockMvc.perform(post("/api/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(d)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Waitlist is disabled"));

        // notify endpoint (service call was removed)
        mockMvc.perform(post("/api/entries/abc123/notify"))
                .andExpect(status().isOk());

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
        when(entryService.calculateEstimatedWaitMinutes()).thenReturn(42);
        mockMvc.perform(post("/api/entries/abc123/sms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isOk());
        verify(entryService).sendSmsToEntry("abc123", "Hello");
        // ensure SSE event was sent and wait calculation was invoked
        verify(entryService).calculateEstimatedWaitMinutes();
        verify(mockEmitter).send(Mockito.any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    public void createEntry_emitsNewEventWithEstimate() throws Exception {
        SseEmitter mockEmitter = Mockito.mock(SseEmitter.class);
        EntryController ctrl = this.applicationContext.getBean(EntryController.class);
        ctrl.emitters.add(mockEmitter);

        Entry a = new Entry("B","222",2);
        try {
            java.lang.reflect.Field f = Entry.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(a, 2L);
        } catch (Exception ignored) {}
        when(entryService.create(any())).thenReturn(a);
        when(entryService.calculateEstimatedWaitMinutes()).thenReturn(99);

        mockMvc.perform(post("/api/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(a)))
                .andExpect(status().isOk());
        verify(entryService).calculateEstimatedWaitMinutes();
        verify(mockEmitter).send(Mockito.any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    public void deleteEntry_emitsDeletedEventWithEstimate() throws Exception {
        SseEmitter mockEmitter = Mockito.mock(SseEmitter.class);
        EntryController ctrl = this.applicationContext.getBean(EntryController.class);
        ctrl.emitters.add(mockEmitter);

        Entry a = new Entry("C","333",3);
        a.setCode("xyz123");
        when(entryService.deleteEntry(anyString())).thenReturn(a);
        when(entryService.calculateEstimatedWaitMinutes()).thenReturn(7);

        mockMvc.perform(delete("/api/entries/xyz123"))
                .andExpect(status().isOk());
        verify(entryService).calculateEstimatedWaitMinutes();
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
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith("text/event-stream")));
    }

    @Test
    public void notifyEntry_emitsNotifiedEvent() throws Exception {
        // register a mock emitter to capture sends
        SseEmitter mockEmitter = Mockito.mock(SseEmitter.class);
        EntryController ctrl = this.applicationContext.getBean(EntryController.class);
        ctrl.emitters.add(mockEmitter);

        mockMvc.perform(post("/api/entries/xyz789/notify"))
                .andExpect(status().isOk());
        // verify emitter.send was invoked with any builder
        verify(mockEmitter).send(Mockito.any(SseEmitter.SseEventBuilder.class));
    }
}
