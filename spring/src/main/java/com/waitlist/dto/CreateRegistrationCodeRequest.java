package com.waitlist.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateRegistrationCodeRequest {
    private Long accountId;
    private String title;
    
    @NotBlank(message = "Role is required")
    private String role;
    
    private String message;

    public CreateRegistrationCodeRequest() {
    }

    public CreateRegistrationCodeRequest(Long accountId) {
        this.accountId = accountId;
    }

    public CreateRegistrationCodeRequest(Long accountId, String title, String role) {
        this.accountId = accountId;
        this.title = title;
        this.role = role;
    }

    public CreateRegistrationCodeRequest(Long accountId, String title, String role, String message) {
        this.accountId = accountId;
        this.title = title;
        this.role = role;
        this.message = message;
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
