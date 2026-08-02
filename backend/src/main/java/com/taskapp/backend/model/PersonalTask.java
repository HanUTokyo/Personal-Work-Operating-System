package com.taskapp.backend.model;

import java.time.LocalDateTime;

public class PersonalTask {

    private Long id;
    private PersonalTaskType taskType;
    private String content;
    private boolean completed;
    private boolean pinned;
    private int sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PersonalTaskType getTaskType() { return taskType; }
    public void setTaskType(PersonalTaskType taskType) { this.taskType = taskType; }
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
