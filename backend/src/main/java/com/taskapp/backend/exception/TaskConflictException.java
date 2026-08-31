package com.taskapp.backend.exception;

import com.taskapp.backend.dto.TaskConflictResponse;

public class TaskConflictException extends RuntimeException {
    private final TaskConflictResponse conflict;
    public TaskConflictException() {
        this(null);
    }
    public TaskConflictException(TaskConflictResponse conflict) {
        super("This project was changed by another collaborator. Your unsaved changes were kept safely as a draft.");
        this.conflict = conflict;
    }
    public TaskConflictResponse getConflict() { return conflict; }
}
