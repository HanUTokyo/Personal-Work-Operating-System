package com.taskapp.backend.dto;

import com.taskapp.backend.model.PersonalTaskType;
import java.time.LocalDateTime;

public class PersonalTaskResponse {

    private Long id;
    private PersonalTaskType type;
    private String content;
    private boolean completed;
    private boolean pinned;
    private int sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PersonalTaskType getType() { return type; }
    public void setType(PersonalTaskType type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
