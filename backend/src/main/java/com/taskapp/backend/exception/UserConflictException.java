package com.taskapp.backend.exception;

public class UserConflictException extends RuntimeException {

    public UserConflictException(String username) {
        super("User already exists: " + username);
    }
}
