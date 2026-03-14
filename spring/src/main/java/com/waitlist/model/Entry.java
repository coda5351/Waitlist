package com.waitlist.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "entries")
public class Entry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // publicly visible random code used instead of numeric id
    @Column(unique = true, nullable = false, length = 6)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
    private Account account;

    private String name;
    private String phone;
    @Column(name = "party_size")
    private int partySize;

    private LocalDateTime timestamp;

    private boolean called;

    @Column(nullable = false)
    private boolean active = true;

    // tracks whether the entry was created from the web UI or the flutter client
    @Column(nullable = false, length = 20)
    @jakarta.persistence.Convert(converter = EntrySourceConverter.class)
    private EntrySource source = EntrySource.WEB;

    public Entry() {
        this.timestamp = LocalDateTime.now();
        this.called = false;
        ensureCode();
    }

    public Entry(String name, String phone, int partySize) {
        this();
        this.name = name;
        this.phone = phone;
        this.partySize = partySize;
    }


    // optionally mark entry inactive
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // generate/ensure code and source when the object is persisted or updated
    // also normalize phone before saving to the DB
    @PrePersist
    @PreUpdate
    public void ensureCode() {
        if (this.code == null || this.code.isBlank()) {
            this.code = generateCode();
        }
        if (this.source == null) {
            this.source = EntrySource.WEB;
        }
        normalizePhone();
    }

    // strip non-digit characters from phone number
    private void normalizePhone() {
        if (this.phone != null) {
            this.phone = this.phone.replaceAll("\\D", "");
        }
    }

    private static String generateCode() {
        return CodeGenerator.generateCode();
    }
}
