package com.waitlist.dto;

public class SendRegistrationCodeEmailRequest {
    private String email;
    private String registrationCode;

    public SendRegistrationCodeEmailRequest() {
    }

    public SendRegistrationCodeEmailRequest(String email, String registrationCode) {
        this.email = email;
        this.registrationCode = registrationCode;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRegistrationCode() {
        return registrationCode;
    }

    public void setRegistrationCode(String registrationCode) {
        this.registrationCode = registrationCode;
    }
}
