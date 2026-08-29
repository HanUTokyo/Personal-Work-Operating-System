package com.taskapp.backend.dto;

import java.time.LocalDateTime;

public class TaskVersionResponse {
    private Long id;
    private long revision;
    private String changeReason;
    private String changedBy;
    private LocalDateTime createdAt;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getRevision() { return revision; }
    public void setRevision(long revision) { this.revision = revision; }
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
