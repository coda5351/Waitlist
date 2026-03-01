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

    private String name;
    private String phone;
    @Column(name = "party_size")
    private int partySize;

    private LocalDateTime timestamp;

    private boolean called;

    @Column(nullable = false)
    private boolean active = true;

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

    // generate/ensure code when the object is persisted or constructed
    @PrePersist
    public void ensureCode() {
        if (this.code == null || this.code.isBlank()) {
            this.code = generateCode();
        }
    }

    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final java.security.SecureRandom CODE_RANDOM = new java.security.SecureRandom();
    private static String generateCode() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            int idx = CODE_RANDOM.nextInt(CODE_CHARS.length());
            sb.append(CODE_CHARS.charAt(idx));
        }
        return sb.toString();
    }
}
