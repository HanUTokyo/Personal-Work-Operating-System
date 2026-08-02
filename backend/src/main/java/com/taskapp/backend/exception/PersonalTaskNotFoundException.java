package com.taskapp.backend.exception;

public class PersonalTaskNotFoundException extends RuntimeException {

    public PersonalTaskNotFoundException(Long taskId) {
        super("Personal task not found with id: " + taskId);
    }
}
