package com.taskapp.backend.dto;

import com.taskapp.backend.model.SharePermission;

public class TaskShareResponse {

    private Long id;
    private Long taskId;
    private UserResponse sharedWith;
    private SharePermission permission;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public UserResponse getSharedWith() {
        return sharedWith;
    }

    public void setSharedWith(UserResponse sharedWith) {
        this.sharedWith = sharedWith;
    }

    public SharePermission getPermission() {
        return permission;
    }

    public void setPermission(SharePermission permission) {
        this.permission = permission;
    }
}
