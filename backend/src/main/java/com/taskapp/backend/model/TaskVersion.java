package com.taskapp.backend.model;

import java.time.LocalDateTime;

public class TaskVersion {
    private Long id;
    private Long taskId;
    private long revision;
    private String snapshotJson;
    private String changeReason;
    private Long changedByUserId;
    private String changedByUsername;
    private LocalDateTime createdAt;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public long getRevision() { return revision; }
    public void setRevision(long revision) { this.revision = revision; }
    public String getSnapshotJson() { return snapshotJson; }
    public void setSnapshotJson(String snapshotJson) { this.snapshotJson = snapshotJson; }
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
    public Long getChangedByUserId() { return changedByUserId; }
    public void setChangedByUserId(Long changedByUserId) { this.changedByUserId = changedByUserId; }
    public String getChangedByUsername() { return changedByUsername; }
    public void setChangedByUsername(String changedByUsername) { this.changedByUsername = changedByUsername; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
