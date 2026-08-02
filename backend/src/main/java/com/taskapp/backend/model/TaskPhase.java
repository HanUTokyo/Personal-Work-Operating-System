package com.taskapp.backend.model;

import java.time.LocalDateTime;

public class TaskPhase {

    private Long id;
    private Long taskId;
    private String phaseKey;
    private String parentPhaseKey;
    private String phaseName;
    private String phaseDescription;
    private PhaseStatus phaseStatus;
    private int sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getPhaseKey() {
        return phaseKey;
    }

    public void setPhaseKey(String phaseKey) {
        this.phaseKey = phaseKey;
    }

    public String getParentPhaseKey() {
        return parentPhaseKey;
    }

    public void setParentPhaseKey(String parentPhaseKey) {
        this.parentPhaseKey = parentPhaseKey;
    }

    public String getPhaseName() {
        return phaseName;
    }

    public void setPhaseName(String phaseName) {
        this.phaseName = phaseName;
    }

    public PhaseStatus getPhaseStatus() {
        return phaseStatus;
    }

    public void setPhaseStatus(PhaseStatus phaseStatus) {
        this.phaseStatus = phaseStatus;
    }

    public String getPhaseDescription() {
        return phaseDescription;
    }

    public void setPhaseDescription(String phaseDescription) {
        this.phaseDescription = phaseDescription;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

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
}
