package com.taskapp.backend.dto;

import com.taskapp.backend.model.SharePermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TaskShareRequest {

    @NotBlank(message = "username is required")
    @Size(max = 60, message = "username must be at most 60 characters")
    private String username;

    @NotNull(message = "permission is required")
    private SharePermission permission;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public SharePermission getPermission() {
        return permission;
    }

    public void setPermission(SharePermission permission) {
        this.permission = permission;
    }
}
