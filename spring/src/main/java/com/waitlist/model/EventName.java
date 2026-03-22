package com.waitlist.model;

/**
 * Event names for waitlist lifecycle SSE events.
 */
public enum EventName {
    NEW_ENTRY("new-entry", "New Entry", "You have been added to the waitlist.", "Please wait; we will notify you when your table is ready."),
    DELETED_ENTRY("deleted-entry", "Left Waitlist", "You have been removed from the waitlist.", "If you were recently called, please contact the host. Otherwise, feel free to join again!"),
    UPDATED_ENTRY("updated-entry", "Table Ready", "Your waitlist entry has been updated.", "Your table has already been called or you are not on the list. Sign up again to join the wait list."),
    NOTIFIED_ENTRY("notified-entry", "Table Ready", "Your table is ready, please proceed to the host stand.", null),
    WAITLIST_DISABLED("waitlist-disabled", "Waitlist Closed", "The waitlist has been closed by the host.", "Hope to see you again when we reopen!");

    private final String value;
    private final String title;
    private final String message;
    private final String subMessage;

    EventName(String value, String title, String message, String subMessage) {
        this.value = value;
        this.title = title;
        this.message = message;
        this.subMessage = subMessage;
    }

    public String getValue() {
        return value;
    }

    public String getMessage() {
        return message;
    }

    public String getSubMessage() {
        return subMessage;
    }

    public String getTitle() {
        return title;
    }

    public static boolean isValid(String value) {
        for (EventName e : values()) {
            if (e.value.equals(value)) {
                return true;
            }
        }
        throw new IllegalArgumentException("Unsupported event name: " + value);
    }
}
