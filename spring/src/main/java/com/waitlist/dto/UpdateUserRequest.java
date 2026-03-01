package com.waitlist.dto;

import com.waitlist.model.User;

public class UpdateUserRequest {
    private String email;
    private String fullName;
    private String password;
    private User.UserRole role;
    private String language;

    private Long defaultBoardId;

    public UpdateUserRequest() {
    }

    public UpdateUserRequest(String email, String fullName, String password, User.UserRole role) {
        this.email = email;
        this.fullName = fullName;
        this.password = password;
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public User.UserRole getRole() {
        return role;
    }

    public void setRole(User.UserRole role) {
        this.role = role;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Long getDefaultBoardId() {
        return defaultBoardId;
    }

    public void setDefaultBoardId(Long defaultBoardId) {
        this.defaultBoardId = defaultBoardId;
    }
}
