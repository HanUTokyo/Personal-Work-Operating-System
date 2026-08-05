package com.taskapp.backend.model;

import java.time.LocalDateTime;

public class TaskKnowledge {

    private Long taskId;
    private String recentDecisions;
    private String recentExperiments;
    private String knowledgeHighlights;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
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
