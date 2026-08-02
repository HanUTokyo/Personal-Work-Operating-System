package com.taskapp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class GlobalAiSuggestionCreateRequest {

    @NotBlank(message = "content is required")
    @Size(max = 20000, message = "content must be at most 20000 characters")
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
