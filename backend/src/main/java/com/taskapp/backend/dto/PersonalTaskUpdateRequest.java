package com.taskapp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PersonalTaskUpdateRequest {

    @NotBlank(message = "content is required")
    @Size(max = 20000, message = "content must be at most 20000 characters")
    private String content;

    @NotNull(message = "completed is required")
    private Boolean completed;

    private Boolean pinned;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }
    public Boolean getPinned() { return pinned; }
    public void setPinned(Boolean pinned) { this.pinned = pinned; }
}
