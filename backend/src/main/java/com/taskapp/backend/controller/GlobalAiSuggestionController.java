package com.taskapp.backend.controller;

import com.taskapp.backend.dto.ApiResponse;
import com.taskapp.backend.dto.GlobalAiSuggestionCreateRequest;
import com.taskapp.backend.dto.GlobalAiSuggestionResponse;
import com.taskapp.backend.service.GlobalAiSuggestionService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai-suggestions")
public class GlobalAiSuggestionController {

    private final GlobalAiSuggestionService service;

    public GlobalAiSuggestionController(GlobalAiSuggestionService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GlobalAiSuggestionResponse>>> getAll(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        return ResponseEntity.ok(ApiResponse.success("AI suggestions fetched successfully", service.getAll(authorizationHeader)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GlobalAiSuggestionResponse>> create(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody GlobalAiSuggestionCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("AI suggestion created successfully", service.create(authorizationHeader, request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GlobalAiSuggestionResponse>> update(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long id,
            @Valid @RequestBody GlobalAiSuggestionCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("AI suggestion updated successfully", service.update(authorizationHeader, id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long id
    ) {
        service.delete(authorizationHeader, id);
        return ResponseEntity.ok(ApiResponse.success("AI suggestion deleted successfully", null));
    }
}
