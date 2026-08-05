package com.taskapp.backend.controller;

import com.taskapp.backend.dto.ApiResponse;
import com.taskapp.backend.dto.GlobalAiSuggestionCreateRequest;
import com.taskapp.backend.dto.GlobalAiSuggestionResponse;
import com.taskapp.backend.service.GlobalAiSuggestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/current-action-goals")
public class GlobalActionGoalController {
    private static final String TYPE = "ACTION_GOAL";
    private final GlobalAiSuggestionService service;
    public GlobalActionGoalController(GlobalAiSuggestionService service) { this.service = service; }
    @GetMapping public ResponseEntity<ApiResponse<List<GlobalAiSuggestionResponse>>> getAll(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(ApiResponse.success("Current action goals fetched successfully", service.getAll(authorization, TYPE)));
    }
    @PostMapping public ResponseEntity<ApiResponse<GlobalAiSuggestionResponse>> create(@RequestHeader(value = "Authorization", required = false) String authorization, @Valid @RequestBody GlobalAiSuggestionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Current action goal created successfully", service.create(authorization, request, TYPE)));
    }
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<GlobalAiSuggestionResponse>> update(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long id, @Valid @RequestBody GlobalAiSuggestionCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Current action goal updated successfully", service.update(authorization, id, request, TYPE)));
    }
    @DeleteMapping("/{id}") public ResponseEntity<ApiResponse<Void>> delete(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long id) {
        service.delete(authorization, id, TYPE); return ResponseEntity.ok(ApiResponse.success("Current action goal deleted successfully", null));
    }
}
