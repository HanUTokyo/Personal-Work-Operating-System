package com.taskapp.backend.dto;

import com.taskapp.backend.model.PersonalTaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PersonalTaskCreateRequest {

    @NotNull(message = "type is required")
    private PersonalTaskType type;

    @NotBlank(message = "content is required")
    @Size(max = 20000, message = "content must be at most 20000 characters")
    private String content;

    public PersonalTaskType getType() { return type; }
    public void setType(PersonalTaskType type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
