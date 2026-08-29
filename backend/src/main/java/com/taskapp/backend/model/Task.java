package com.taskapp.backend.model;

import java.time.LocalDateTime;

public class Task {

    private Long id;
    private String taskTitle;
    private String taskDescription;
    private Long ownerUserId;
    private ProjectPriority priority;
    private PhaseStatus phase1Status;
    private PhaseStatus phase2Status;
    private PhaseStatus phase3Status;
    private double overallProgress;
    private boolean pinned;
    private boolean archived;
    private LocalDateTime archivedAt;
    private boolean demo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long revision;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public ProjectPriority getPriority() {
        return priority;
    }

    public void setPriority(ProjectPriority priority) {
        this.priority = priority;
    }

    public PhaseStatus getPhase1Status() {
        return phase1Status;
    }

    public void setPhase1Status(PhaseStatus phase1Status) {
        this.phase1Status = phase1Status;
    }

    public PhaseStatus getPhase2Status() {
        return phase2Status;
    }

    public void setPhase2Status(PhaseStatus phase2Status) {
        this.phase2Status = phase2Status;
    }

    public PhaseStatus getPhase3Status() {
        return phase3Status;
    }

    public void setPhase3Status(PhaseStatus phase3Status) {
        this.phase3Status = phase3Status;
    }

    public double getOverallProgress() {
        return overallProgress;
    }

    public void setOverallProgress(double overallProgress) {
        this.overallProgress = overallProgress;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public LocalDateTime getArchivedAt() { return archivedAt; }
    public void setArchivedAt(LocalDateTime archivedAt) { this.archivedAt = archivedAt; }
    public boolean isDemo() { return demo; }
    public void setDemo(boolean demo) { this.demo = demo; }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getRevision() { return revision; }
    public void setRevision(long revision) { this.revision = revision; }
}
