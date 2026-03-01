package com.waitlist.dto;

public class SupportEmailRequest {
    private String name;
    private String email;
    private String subject;
    private String message;
    private String phoneNumber;
    private Boolean okToText;

    // Constructors
    public SupportEmailRequest() {
    }

    public SupportEmailRequest(String name, String email, String subject, String message) {
        this.name = name;
        this.email = email;
        this.subject = subject;
        this.message = message;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Boolean getOkToText() {
        return okToText;
    }

    public void setOkToText(Boolean okToText) {
        this.okToText = okToText;
    }
}
