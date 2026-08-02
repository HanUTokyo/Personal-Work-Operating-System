package com.taskapp.backend.exception;

public class GlobalAiSuggestionNotFoundException extends RuntimeException {

    public GlobalAiSuggestionNotFoundException(Long suggestionId) {
        super("Global AI suggestion not found with id: " + suggestionId);
    }
}
