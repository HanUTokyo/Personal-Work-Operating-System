package com.taskapp.backend.dto;

import jakarta.validation.constraints.NotNull;

public class TaskPinRequest {

    @NotNull(message = "pinned is required")
    private Boolean pinned;

    public Boolean getPinned() {
        return pinned;
    }

    public void setPinned(Boolean pinned) {
        this.pinned = pinned;
    }
}
