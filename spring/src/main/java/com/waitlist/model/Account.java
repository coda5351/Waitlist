package com.waitlist.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_name", nullable = false)
    private String accountName;

    @Column(name = "branding_color_code", length = 30)
    private String brandingColorCode;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // waitlist configuration
    @Column(name = "waitlist_enabled", nullable = false)
    private boolean waitlistEnabled = false;

    // toggle whether Twilio SMS messages should actually be sent.  When false
    // we simply log attempts, which makes development/testing easier and
    // allows administrators to temporarily disable notifications.
    @Column(name = "sms_enabled", nullable = false)
    private boolean smsEnabled = true;

    @Column(name = "waitlist_open_time")
    private LocalDateTime waitlistOpenTime;

    @Column(name = "waitlist_close_time")
    private LocalDateTime waitlistCloseTime;

    // service hours per day of week; used as default when explicit open/close
    // times are not provided.  Stored in a separate collection table.
    @jakarta.persistence.ElementCollection
    @jakarta.persistence.CollectionTable(name = "account_service_hours",
            joinColumns = @jakarta.persistence.JoinColumn(name = "account_id"))
    @jakarta.persistence.MapKeyColumn(name = "day_of_week")
    @jakarta.persistence.MapKeyEnumerated(jakarta.persistence.EnumType.STRING)
    private java.util.Map<java.time.DayOfWeek, ServiceHours> serviceHours = new java.util.HashMap<>();

    // twilio credentials -- moved from properties into account-specific settings
    @Column(name = "twilio_account_sid")
    private String twilioAccountSid;

    @Column(name = "twilio_auth_token")
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    private String twilioAuthToken;

    // customizable message templates keyed by an arbitrary string.  This map is
    // exposed via the admin UI so account owners can tweak notifications
    // without changing code.  The map is stored in a separate collection table.
    @jakarta.persistence.ElementCollection
    @jakarta.persistence.CollectionTable(name = "account_messages",
            joinColumns = @jakarta.persistence.JoinColumn(name = "account_id"))
    @jakarta.persistence.MapKeyColumn(name = "message_key")
    @Column(name = "template")
    private java.util.Map<String,String> messages = new java.util.HashMap<>();

    public Account() {
        // default constructor leaves boolean defaults (waitlistEnabled=false, smsEnabled=true)
    }

    public Account(String accountName, String brandingColorCode) {
        this.accountName = accountName;
        this.brandingColorCode = brandingColorCode;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getBrandingColorCode() {
        return brandingColorCode;
    }

    public void setBrandingColorCode(String brandingColorCode) {
        this.brandingColorCode = brandingColorCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // waitlist accessors
    public boolean isWaitlistEnabled() {
        return waitlistEnabled;
    }

    public void setWaitlistEnabled(boolean waitlistEnabled) {
        this.waitlistEnabled = waitlistEnabled;
    }

    public LocalDateTime getWaitlistOpenTime() {
        return waitlistOpenTime;
    }

    public void setWaitlistOpenTime(LocalDateTime waitlistOpenTime) {
        this.waitlistOpenTime = waitlistOpenTime;
    }

    public LocalDateTime getWaitlistCloseTime() {
        return waitlistCloseTime;
    }

    public boolean isSmsEnabled() {
        return smsEnabled;
    }

    public void setSmsEnabled(boolean smsEnabled) {
        this.smsEnabled = smsEnabled;
    }

    public void setWaitlistCloseTime(LocalDateTime waitlistCloseTime) {
        this.waitlistCloseTime = waitlistCloseTime;
    }

    public java.util.Map<java.time.DayOfWeek, ServiceHours> getServiceHours() {
        return serviceHours;
    }

    public void setServiceHours(java.util.Map<java.time.DayOfWeek, ServiceHours> serviceHours) {
        this.serviceHours = serviceHours;
    }

    public String getTwilioAccountSid() {
        return twilioAccountSid;
    }

    public void setTwilioAccountSid(String twilioAccountSid) {
        this.twilioAccountSid = twilioAccountSid;
    }

    // return placeholder when token exists; field itself is write-only
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    public String getTwilioAuthToken() {
        if (twilioAuthToken != null && !twilioAuthToken.isBlank()) {
            return "*************";
        }
        return null;
    }

    public void setTwilioAuthToken(String twilioAuthToken) {
        this.twilioAuthToken = twilioAuthToken;
    }

    /**
     * Internal accessor used by server code to retrieve the actual token.
     * This is ignored during JSON serialization so it cannot be leaked.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getRawTwilioAuthToken() {
        return twilioAuthToken;
    }

    // message template accessors
    public java.util.Map<String, String> getMessages() {
        return messages;
    }

    public void setMessages(java.util.Map<String, String> messages) {
        this.messages = messages;
    }
}
