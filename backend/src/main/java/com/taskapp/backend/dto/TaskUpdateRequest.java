package com.taskapp.backend.dto;

import com.taskapp.backend.model.ProjectPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class TaskUpdateRequest {

    @NotBlank(message = "taskTitle is required")
    @Size(max = 200, message = "taskTitle must be at most 200 characters")
    private String taskTitle;

    @Size(max = 2000, message = "taskDescription must be at most 2000 characters")
    private String taskDescription;

    @Size(max = 20000, message = "recentDecisions must be at most 20000 characters")
    private String recentDecisions;

    @Size(max = 20000, message = "recentExperiments must be at most 20000 characters")
    private String recentExperiments;

    @Size(max = 20000, message = "knowledgeHighlights must be at most 20000 characters")
    private String knowledgeHighlights;

    @Size(max = 20000, message = "currentActionGoal must be at most 20000 characters")
    private String currentActionGoal;

    private ProjectPriority priority;

    @Valid
    private List<PhaseRequest> phases = new ArrayList<>();

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
    public String getCurrentActionGoal() { return currentActionGoal; }
    public void setCurrentActionGoal(String currentActionGoal) { this.currentActionGoal = currentActionGoal; }

    public ProjectPriority getPriority() {
        return priority;
    }

    public void setPriority(ProjectPriority priority) {
        this.priority = priority;
    }

    public List<PhaseRequest> getPhases() {
        return phases;
    }

    public void setPhases(List<PhaseRequest> phases) {
        this.phases = phases;
    }
}
