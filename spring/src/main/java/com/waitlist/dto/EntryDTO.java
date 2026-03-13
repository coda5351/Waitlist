package com.waitlist.dto;

import java.time.LocalDateTime;

/**
 * Transport object representing a waitlist entry with only the owning
 * account's identifier rather than the full account object.  Used for
 * serialization over the REST API.
 */
public class EntryDTO {
    private Long id;
    private String code;
    private Long accountId;
    private String name;
    private String phone;
    private com.waitlist.model.EntrySource source;
    private int partySize;
    private LocalDateTime timestamp;
    private boolean called;
    private boolean active;

    // getters/setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public com.waitlist.model.EntrySource getSource() {
        return source;
    }

    public void setSource(com.waitlist.model.EntrySource source) {
        this.source = source;
    }

    public int getPartySize() {
        return partySize;
    }

    public void setPartySize(int partySize) {
        this.partySize = partySize;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isCalled() {
        return called;
    }

    public void setCalled(boolean called) {
        this.called = called;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
