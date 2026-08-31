package com.taskapp.backend.dto;

import java.time.LocalDateTime;

public class TaskConflictDraftResponse {
    private Long id;
    private TaskUpdateRequest payload;
    private LocalDateTime createdAt;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TaskUpdateRequest getPayload() { return payload; }
    public void setPayload(TaskUpdateRequest payload) { this.payload = payload; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
