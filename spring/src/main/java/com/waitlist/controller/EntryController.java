package com.waitlist.controller;

import com.twilio.exception.ApiException;
import com.waitlist.model.Entry;
import com.waitlist.dto.EntryDTO;
import com.waitlist.service.EntryService;
import com.waitlist.service.MessageTemplate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/entries")
public class EntryController {

    private static final Logger logger = LoggerFactory.getLogger(EntryController.class);

    private enum EventName {
        NEW_ENTRY("new-entry"),
        DELETED_ENTRY("deleted-entry"),
        UPDATED_ENTRY("updated-entry"),
        NOTIFIED_ENTRY("notified-entry"),
        WAITLIST_DISABLED("waitlist-disabled");

        private final String value;

        EventName(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        /**
         * Determine whether the provided value matches a known event name.
         */
        public static boolean isValid(String value) {
            for (EventName e : values()) {
                if (e.value.equals(value)) {
                    return true;
                }
            }
            throw new IllegalArgumentException("Unsupported event name: " + value);
        }
    }

    @Autowired
    private EntryService entryService;

    // list of active Server-Sent Event emitters; CopyOnWriteArrayList makes concurrent
    // modifications safe and we remove dead emitters as they timeout or complete.
    // package-visible so tests can register mock emitters
    final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    private void sendEvent(EventName eventName, Object data) {
        // validate event name even though the type is an enum; this is defensive in case
        // someone uses the string-based overload in tests or future extensions.
        if (eventName == null || !EventName.isValid(eventName.getValue())) {
            throw new IllegalArgumentException("Event name must valid and not null");
        }

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                SseEmitter.SseEventBuilder builder = SseEmitter.event()
                        .name(eventName.getValue())
                        .data(data, MediaType.APPLICATION_JSON);
                emitter.send(builder);
            } catch (IOException e) {
                // client likely closed connection; log at warn level without stack trace
                logger.warn("Failed to send SSE event '{}' to client: {}", eventName.getValue(), e.getMessage());
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }

    private void emitToEntry(String entryCode, EventName eventName) {
        EntryDTO dto = entryService.toDto(entryService.resolve(entryCode));
        Map<String,Object> payload = Map.of(
                "entry", dto,
                "estimatedWait", entryService.calculateEstimatedWaitMinutes(entryService.getAllActiveEntriesForAccount(dto.getAccountId()))
        );
        sendEvent(eventName, payload);
    }

    /**
     * Notify clients that the entire waitlist has been disabled (closed).
     */
    public void emitWaitlistDisabled() {
        sendEvent(EventName.WAITLIST_DISABLED, "");
    }

    /**
     * Server-sent event stream that pushes updates when entries change.
     */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEntries() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        // remove on completion/timeout
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitters.add(emitter);
        return emitter;
    }

    @PostMapping("/create/{code}")
    public ResponseEntity<EntryDTO> createEntry(@PathVariable String code, @RequestBody Entry entry) {
        Entry saved = entryService.create(code, entry);
        // push update so clients can append the new entry without polling
        emitToEntry(saved.getCode(), EventName.NEW_ENTRY);
        return ResponseEntity.ok(entryService.toDto(saved));
    }

    @PostMapping("/{code}/notify")
    public ResponseEntity<Void> notifyEntry(@PathVariable String code, @RequestBody Map<String,String> req) {

        // Here we lay the ground work but this could serve to notify of other events as well (e.g. reservation confirmed, table ready, etc.) 
        // by including a "type" field in the request and switching on that to determine the message template and whether to send an SMS or just a
        // generic notification for the front end to handle (e.g. show a popup).  
        // For now we only have the one "tableReady" type which sends an SMS using the existing notifyOfTableSms method but we can expand this 
        // in the future as needed.
        if(
            req.containsKey("type") && req.get("type").equals("sms") && 
            req.containsKey("message") && req.get("message").equals("tableReady")
        ) {
            // send a generic notification (e.g. for front end to show a popup) without sending an SMS
            entryService.notifyOfTableSms(null, code, MessageTemplate.TABLE_READY);
        }
        // notify listening clients that this entry has been called
        emitToEntry(code, EventName.NOTIFIED_ENTRY);
        return ResponseEntity.ok().build();
    }

    /**
     * Send a text message to the entry's phone number using Twilio.
     * Request body should be a JSON object with a "message" field.
     */
    @PostMapping("/{code}/sms")
    public ResponseEntity<Void> sendSmsToEntry(@PathVariable String code, @RequestBody Map<String,String> req) {
        String message = req.getOrDefault("message", "");
        entryService.sendSmsToEntry(code, message);
        // notify listening clients that this entry has been contacted via SMS
        emitToEntry(code, EventName.NOTIFIED_ENTRY);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> deleteEntry(@PathVariable String code) {
        emitToEntry(code, EventName.DELETED_ENTRY);
        entryService.deleteEntry(code);       
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{code}/called")
    public ResponseEntity<EntryDTO> setCalled(@PathVariable String code, @RequestBody Map<String,Boolean> req) {
        boolean called = req.getOrDefault("called", false);
        Entry updated = entryService.markCalled(code, called);
        emitToEntry(code, EventName.UPDATED_ENTRY);
        return ResponseEntity.ok(entryService.toDto(updated));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Void> handleIOException(IOException ex) {
        // client disconnected or other IO issue; log a warning and return no content
        logger.warn("IO exception in request: {}", ex.getMessage());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<String> handleApiException(ApiException ex) {
        // Twilio API exception; log at error level without stack trace since these are often due to client issues (e.g. invalid phone number)
        logger.error("API exception in request: {}", ex.getMessage());
        // return message in body so front end can show error details
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
