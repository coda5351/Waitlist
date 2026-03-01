package com.waitlist.service;

/**
 * Represents a message template key along with its built-in default value.
 *
 * Entries may override the message by storing a custom template in the
 * {@code account_message_template} table; the key is used to look up the
 * stored value.  If none is present the {@link #getDefaultTemplate default}
 * text will be used.
 */
public enum MessageTemplate {
    NEW_ENTRY("newEntry", "Hello %s,%n%n" +
            "Thank you for joining the waitlist. " +
            "Your current position will be visible at %s/%s%n%n" +
            "Details:%n - Name: %s%n - Phone: %s%n - Party size: %d%n"),
    TABLE_READY("tableReady", "Hello %s,%n%n" +
            "Your table is ready! Please proceed to the host stand.%n%n" +
            "Details:%n - Name: %s%n - Phone: %s%n - Party size: %d%n");

    private final String key;
    private final String defaultTemplate;

    MessageTemplate(String key, String defaultTemplate) {
        this.key = key;
        this.defaultTemplate = defaultTemplate;
    }

    /**
     * The string that is stored in the database for this template.
     */
    public String getKey() {
        return key;
    }

    /**
     * The built-in template used when no custom value has been configured.
     */
    public String getDefaultTemplate() {
        return defaultTemplate;
    }
}
