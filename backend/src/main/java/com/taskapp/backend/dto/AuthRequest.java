package com.taskapp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthRequest {

    @NotBlank(message = "username is required")
    @Size(max = 60, message = "username must be at most 60 characters")
    private String username;

    @NotBlank(message = "password is required")
    @Size(max = 200, message = "password must be at most 200 characters")
    private String password;

    @Size(max = 100, message = "displayName must be at most 100 characters")
    private String displayName;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
