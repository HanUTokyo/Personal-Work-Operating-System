package com.taskapp.backend.controller;

import com.taskapp.backend.dto.ApiResponse;
import com.taskapp.backend.dto.TaskCreateRequest;
import com.taskapp.backend.dto.TaskNoteCreateRequest;
import com.taskapp.backend.dto.TaskNoteResponse;
import com.taskapp.backend.dto.TaskResponse;
import com.taskapp.backend.dto.TaskShareRequest;
import com.taskapp.backend.dto.TaskShareResponse;
import com.taskapp.backend.dto.TaskUpdateRequest;
import com.taskapp.backend.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasks(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String order
    ) {
        List<TaskResponse> tasks = taskService.getAllTasks(authorizationHeader, keyword, sortBy, order);
        return ResponseEntity.ok(ApiResponse.success("Tasks fetched successfully", tasks));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTaskById(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long id
    ) {
        TaskResponse task = taskService.getTaskById(authorizationHeader, id);
        return ResponseEntity.ok(ApiResponse.success("Task fetched successfully", task));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody TaskCreateRequest request
    ) {
        TaskResponse createdTask = taskService.createTask(authorizationHeader, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Task created successfully", createdTask));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long id,
            @Valid @RequestBody TaskUpdateRequest request
    ) {
        TaskResponse updatedTask = taskService.updateTask(authorizationHeader, id, request);
        return ResponseEntity.ok(ApiResponse.success("Task updated successfully", updatedTask));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long id
    ) {
        taskService.deleteTask(authorizationHeader, id);
        return ResponseEntity.ok(ApiResponse.success("Task deleted successfully", null));
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<ApiResponse<TaskNoteResponse>> addTaskNote(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long id,
            @Valid @RequestBody TaskNoteCreateRequest request
    ) {
        TaskNoteResponse createdNote = taskService.addTaskNote(authorizationHeader, id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Task note created successfully", createdNote));
    }

    @PutMapping("/{id}/notes/{noteId}")
    public ResponseEntity<ApiResponse<TaskNoteResponse>> updateTaskNote(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long id,
            @PathVariable Long noteId,
            @Valid @RequestBody TaskNoteCreateRequest request
    ) {
        TaskNoteResponse updatedNote = taskService.updateTaskNote(authorizationHeader, id, noteId, request);
        return ResponseEntity.ok(ApiResponse.success("Task note updated successfully", updatedNote));
    }

    @DeleteMapping("/{id}/notes/{noteId}")
    public ResponseEntity<ApiResponse<Void>> deleteTaskNote(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long id,
            @PathVariable Long noteId
    ) {
        taskService.deleteTaskNote(authorizationHeader, id, noteId);
        return ResponseEntity.ok(ApiResponse.success("Task note deleted successfully", null));
    }

    @GetMapping("/{id}/shares")
    public ResponseEntity<ApiResponse<List<TaskShareResponse>>> getTaskShares(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success("Task shares fetched successfully", taskService.getTaskShares(authorizationHeader, id)));
    }

    @PostMapping("/{id}/shares")
    public ResponseEntity<ApiResponse<TaskShareResponse>> shareTask(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long id,
            @Valid @RequestBody TaskShareRequest request
    ) {
        TaskShareResponse share = taskService.shareTask(authorizationHeader, id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Task shared successfully", share));
    }

    @PutMapping("/{id}/shares/{shareId}")
    public ResponseEntity<ApiResponse<TaskShareResponse>> updateTaskShare(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long id,
            @PathVariable Long shareId,
            @Valid @RequestBody TaskShareRequest request
    ) {
        TaskShareResponse share = taskService.updateTaskShare(authorizationHeader, id, shareId, request);
        return ResponseEntity.ok(ApiResponse.success("Task share updated successfully", share));
    }

    @DeleteMapping("/{id}/shares/{shareId}")
    public ResponseEntity<ApiResponse<Void>> deleteTaskShare(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long id,
            @PathVariable Long shareId
    ) {
        taskService.deleteTaskShare(authorizationHeader, id, shareId);
        return ResponseEntity.ok(ApiResponse.success("Task share deleted successfully", null));
    }
}
