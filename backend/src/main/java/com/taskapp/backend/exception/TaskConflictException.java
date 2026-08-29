package com.taskapp.backend.exception;

public class TaskConflictException extends RuntimeException {
    public TaskConflictException() {
        super("This project was changed by another collaborator. Reload it before saving so no work is overwritten.");
    }
}
