package com.waitlist.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Source of an entry (makes it easy to distinguish web vs. Flutter clients).
 */
public enum EntrySource {
    WEB("web",false, "web"),
    ANDROID_FLUTTER("android_flutter",true, "flutter"),;

    private final String name;
    private final boolean mobile;
    private final String platform;

    EntrySource(String name, boolean mobile, String platform) {
        this.name = name;
        this.mobile = mobile;
        this.platform = platform;
    }

    public boolean isMobile() {
        return mobile;
    }

    public String getPlatform() {
        return platform;
    }

    public String getName() {
        return name;
    }

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static EntrySource from(String value) {
        if (value == null) {
            return null;
        }
        for (EntrySource s : values()) {
            if (s.name().equalsIgnoreCase(value.trim().toLowerCase())) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown entry source: " + value);
    }
}
