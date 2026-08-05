package com.taskapp.backend.service;

import com.taskapp.backend.dto.GlobalAiSuggestionCreateRequest;
import com.taskapp.backend.dto.GlobalAiSuggestionResponse;
import com.taskapp.backend.exception.GlobalAiSuggestionNotFoundException;
import com.taskapp.backend.model.AppUser;
import com.taskapp.backend.model.GlobalAiSuggestion;
import com.taskapp.backend.repository.GlobalAiSuggestionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class GlobalAiSuggestionService {

    private final GlobalAiSuggestionRepository repository;
    private final AuthService authService;

    public GlobalAiSuggestionService(GlobalAiSuggestionRepository repository, AuthService authService) {
        this.repository = repository;
        this.authService = authService;
    }

    public List<GlobalAiSuggestionResponse> getAll(String authorizationHeader) { return getAll(authorizationHeader, "AI"); }
    public List<GlobalAiSuggestionResponse> getAll(String authorizationHeader, String type) {
        AppUser user = authService.requireUser(authorizationHeader);
        return repository.findAll(user.getId(), type).stream().map(this::toResponse).toList();
    }

    public GlobalAiSuggestionResponse create(String authorizationHeader, GlobalAiSuggestionCreateRequest request) { return create(authorizationHeader, request, "AI"); }
    public GlobalAiSuggestionResponse create(String authorizationHeader, GlobalAiSuggestionCreateRequest request, String type) {
        AppUser user = authService.requireUser(authorizationHeader);
        return toResponse(repository.save(user.getId(), type, request.getContent(), now()));
    }

    public GlobalAiSuggestionResponse update(String authorizationHeader, Long suggestionId, GlobalAiSuggestionCreateRequest request) { return update(authorizationHeader, suggestionId, request, "AI"); }
    public GlobalAiSuggestionResponse update(String authorizationHeader, Long suggestionId, GlobalAiSuggestionCreateRequest request, String type) {
        AppUser user = authService.requireUser(authorizationHeader);
        GlobalAiSuggestion suggestion = repository.update(user.getId(), suggestionId, type, request.getContent(), now());
        if (suggestion == null) throw new GlobalAiSuggestionNotFoundException(suggestionId);
        return toResponse(suggestion);
    }

    public void delete(String authorizationHeader, Long suggestionId) { delete(authorizationHeader, suggestionId, "AI"); }
    public void delete(String authorizationHeader, Long suggestionId, String type) {
        AppUser user = authService.requireUser(authorizationHeader);
        if (!repository.softDelete(user.getId(), suggestionId, type, now())) {
            throw new GlobalAiSuggestionNotFoundException(suggestionId);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private GlobalAiSuggestionResponse toResponse(GlobalAiSuggestion suggestion) {
        GlobalAiSuggestionResponse response = new GlobalAiSuggestionResponse();
        response.setId(suggestion.getId());
        response.setContent(suggestion.getContent());
        response.setCreatedAt(suggestion.getCreatedAt());
        response.setUpdatedAt(suggestion.getUpdatedAt());
        return response;
    }
}
