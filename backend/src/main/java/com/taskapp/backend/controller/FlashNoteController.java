package com.taskapp.backend.controller;

import com.taskapp.backend.dto.ApiResponse;
import com.taskapp.backend.dto.FlashNoteCreateRequest;
import com.taskapp.backend.dto.FlashNoteResponse;
import com.taskapp.backend.service.FlashNoteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.util.List;

@RestController
@RequestMapping("/api/flash-notes")
public class FlashNoteController {

    private final FlashNoteService flashNoteService;

    public FlashNoteController(FlashNoteService flashNoteService) {
        this.flashNoteService = flashNoteService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FlashNoteResponse>>> getFlashNotes(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        List<FlashNoteResponse> notes = flashNoteService.getAllFlashNotes(authorizationHeader);
        return ResponseEntity.ok(ApiResponse.success("Flash notes fetched successfully", notes));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FlashNoteResponse>> createFlashNote(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody FlashNoteCreateRequest request
    ) {
        FlashNoteResponse created = flashNoteService.createFlashNote(authorizationHeader, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Flash note created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FlashNoteResponse>> updateFlashNote(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long id,
            @Valid @RequestBody FlashNoteCreateRequest request
    ) {
        FlashNoteResponse updated = flashNoteService.updateFlashNote(authorizationHeader, id, request);
        return ResponseEntity.ok(ApiResponse.success("Flash note updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFlashNote(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long id
    ) {
        flashNoteService.deleteFlashNote(authorizationHeader, id);
        return ResponseEntity.ok(ApiResponse.success("Flash note deleted successfully", null));
    }
}
