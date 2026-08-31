package com.taskapp.backend.dto;

public class TaskConflictResponse {
    private Long draftId;
    private TaskResponse latestTask;
    public Long getDraftId() { return draftId; }
    public void setDraftId(Long draftId) { this.draftId = draftId; }
    public TaskResponse getLatestTask() { return latestTask; }
    public void setLatestTask(TaskResponse latestTask) { this.latestTask = latestTask; }
}
