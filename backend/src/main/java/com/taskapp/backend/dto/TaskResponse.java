package com.taskapp.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class TaskResponse {

    private Long id;
    private String taskTitle;
    private String taskDescription;
    private String recentDecisions;
    private String recentExperiments;
    private String knowledgeHighlights;
    private String priority;
    private Long ownerUserId;
    private String ownerUsername;
    private boolean ownedByCurrentUser;
    private boolean sharedWithCurrentUser;
    private String accessLevel;
    private boolean pinned;
    private boolean archived;
    private LocalDateTime archivedAt;
    private List<PhaseResponse> phases;
    private List<TaskNoteResponse> notes;
    private double overallProgress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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

    public String getRecentDecisions() {
        return recentDecisions;
    }

    public void setRecentDecisions(String recentDecisions) {
        this.recentDecisions = recentDecisions;
    }

    public String getRecentExperiments() {
        return recentExperiments;
    }

    public void setRecentExperiments(String recentExperiments) {
        this.recentExperiments = recentExperiments;
    }

    public String getKnowledgeHighlights() {
        return knowledgeHighlights;
    }

    public void setKnowledgeHighlights(String knowledgeHighlights) {
        this.knowledgeHighlights = knowledgeHighlights;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public boolean isOwnedByCurrentUser() {
        return ownedByCurrentUser;
    }

    public void setOwnedByCurrentUser(boolean ownedByCurrentUser) {
        this.ownedByCurrentUser = ownedByCurrentUser;
    }

    public boolean isSharedWithCurrentUser() {
        return sharedWithCurrentUser;
    }

    public void setSharedWithCurrentUser(boolean sharedWithCurrentUser) {
        this.sharedWithCurrentUser = sharedWithCurrentUser;
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
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

    public List<PhaseResponse> getPhases() {
        return phases;
    }

    public void setPhases(List<PhaseResponse> phases) {
        this.phases = phases;
    }

    public List<TaskNoteResponse> getNotes() {
        return notes;
    }

    public void setNotes(List<TaskNoteResponse> notes) {
        this.notes = notes;
    }

    public double getOverallProgress() {
        return overallProgress;
    }

    public void setOverallProgress(double overallProgress) {
        this.overallProgress = overallProgress;
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
