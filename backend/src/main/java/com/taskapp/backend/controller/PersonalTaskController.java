package com.taskapp.backend.controller;

import com.taskapp.backend.dto.ApiResponse;
import com.taskapp.backend.dto.PersonalTaskCreateRequest;
import com.taskapp.backend.dto.PersonalTaskReorderRequest;
import com.taskapp.backend.dto.PersonalTaskResponse;
import com.taskapp.backend.dto.PersonalTaskUpdateRequest;
import com.taskapp.backend.model.PersonalTaskType;
import com.taskapp.backend.service.PersonalTaskService;
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
@RequestMapping("/api/personal-tasks")
public class PersonalTaskController {

    private final PersonalTaskService service;

    public PersonalTaskController(PersonalTaskService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PersonalTaskResponse>>> getAll(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestParam PersonalTaskType type
    ) {
        return ResponseEntity.ok(ApiResponse.success("Personal tasks fetched successfully", service.getAll(authorizationHeader, type)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PersonalTaskResponse>> create(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody PersonalTaskCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Personal task created successfully", service.create(authorizationHeader, request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PersonalTaskResponse>> update(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long id,
            @Valid @RequestBody PersonalTaskUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Personal task updated successfully", service.update(authorizationHeader, id, request)));
    }

    @PutMapping("/reorder")
    public ResponseEntity<ApiResponse<Void>> reorder(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody PersonalTaskReorderRequest request
    ) {
        service.reorder(authorizationHeader, request);
        return ResponseEntity.ok(ApiResponse.success("Personal tasks reordered successfully", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long id
    ) {
        service.delete(authorizationHeader, id);
        return ResponseEntity.ok(ApiResponse.success("Personal task deleted successfully", null));
    }
}
