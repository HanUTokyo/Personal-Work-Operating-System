package com.taskapp.backend.service;

import com.taskapp.backend.dto.FlashNoteCreateRequest;
import com.taskapp.backend.dto.FlashNoteResponse;
import com.taskapp.backend.exception.FlashNoteNotFoundException;
import com.taskapp.backend.model.AppUser;
import com.taskapp.backend.model.FlashNote;
import com.taskapp.backend.repository.FlashNoteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class FlashNoteService {

    private final FlashNoteRepository flashNoteRepository;
    private final AuthService authService;

    public FlashNoteService(FlashNoteRepository flashNoteRepository, AuthService authService) {
        this.flashNoteRepository = flashNoteRepository;
        this.authService = authService;
    }

    public List<FlashNoteResponse> getAllFlashNotes(String authorizationHeader) {
        AppUser currentUser = authService.requireUser(authorizationHeader);
        return flashNoteRepository.findAll(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public FlashNoteResponse createFlashNote(String authorizationHeader, FlashNoteCreateRequest request) {
        AppUser currentUser = authService.requireUser(authorizationHeader);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        FlashNote saved = flashNoteRepository.save(currentUser.getId(), request.getNoteContent(), now);
        return toResponse(saved);
    }

    public FlashNoteResponse updateFlashNote(String authorizationHeader, Long noteId, FlashNoteCreateRequest request) {
        AppUser currentUser = authService.requireUser(authorizationHeader);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        FlashNote updated = flashNoteRepository.update(currentUser.getId(), noteId, removeDemoPrefix(request.getNoteContent()), now);
        if (updated == null) {
            throw new FlashNoteNotFoundException(noteId);
        }
        return toResponse(updated);
    }

    public void deleteFlashNote(String authorizationHeader, Long noteId) {
        AppUser currentUser = authService.requireUser(authorizationHeader);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        boolean deleted = flashNoteRepository.softDelete(currentUser.getId(), noteId, now);
        if (!deleted) {
            throw new FlashNoteNotFoundException(noteId);
        }
    }

    private FlashNoteResponse toResponse(FlashNote note) {
        FlashNoteResponse response = new FlashNoteResponse();
        response.setId(note.getId());
        response.setNoteContent(note.getNoteContent());
        response.setCreatedAt(note.getCreatedAt());
        response.setUpdatedAt(note.getUpdatedAt());
        return response;
    }
    private String removeDemoPrefix(String value) { return value == null ? null : value.replaceFirst("^\\[demo\\]\\s*", ""); }
}
