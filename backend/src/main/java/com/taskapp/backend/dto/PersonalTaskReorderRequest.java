package com.taskapp.backend.dto;

import com.taskapp.backend.model.PersonalTaskType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class PersonalTaskReorderRequest {

    @NotNull(message = "type is required")
    private PersonalTaskType type;

    @NotEmpty(message = "taskIds is required")
    private List<@NotNull(message = "task id is required") Long> taskIds;

    public PersonalTaskType getType() { return type; }
    public void setType(PersonalTaskType type) { this.type = type; }
    public List<Long> getTaskIds() { return taskIds; }
    public void setTaskIds(List<Long> taskIds) { this.taskIds = taskIds; }
}
