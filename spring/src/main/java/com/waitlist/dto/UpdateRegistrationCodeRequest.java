package com.waitlist.dto;

public class UpdateRegistrationCodeRequest {
    private String registrationCode;
    private Long accountId;
    private String title;
    private String role;
    private String message;

    public UpdateRegistrationCodeRequest() {
    }

    public UpdateRegistrationCodeRequest(String registrationCode, Long accountId) {
        this.registrationCode = registrationCode;
        this.accountId = accountId;
    }

    public UpdateRegistrationCodeRequest(String registrationCode, Long accountId, String title, String role) {
        this.registrationCode = registrationCode;
        this.accountId = accountId;
        this.title = title;
        this.role = role;
    }

    public UpdateRegistrationCodeRequest(String registrationCode, Long accountId, String title, String role, String message) {
        this.registrationCode = registrationCode;
        this.accountId = accountId;
        this.title = title;
        this.role = role;
        this.message = message;
    }

    public String getRegistrationCode() {
        return registrationCode;
    }

    public void setRegistrationCode(String registrationCode) {
        this.registrationCode = registrationCode;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
