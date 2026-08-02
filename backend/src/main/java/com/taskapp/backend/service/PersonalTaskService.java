package com.taskapp.backend.service;

import com.taskapp.backend.dto.PersonalTaskCreateRequest;
import com.taskapp.backend.dto.PersonalTaskReorderRequest;
import com.taskapp.backend.dto.PersonalTaskResponse;
import com.taskapp.backend.dto.PersonalTaskUpdateRequest;
import com.taskapp.backend.exception.PersonalTaskNotFoundException;
import com.taskapp.backend.model.AppUser;
import com.taskapp.backend.model.PersonalTask;
import com.taskapp.backend.model.PersonalTaskType;
import com.taskapp.backend.repository.PersonalTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Service
public class PersonalTaskService {

    private final PersonalTaskRepository repository;
    private final AuthService authService;

    public PersonalTaskService(PersonalTaskRepository repository, AuthService authService) {
        this.repository = repository;
        this.authService = authService;
    }

    public List<PersonalTaskResponse> getAll(String authorizationHeader, PersonalTaskType type) {
        AppUser user = authService.requireUser(authorizationHeader);
        return repository.findAll(user.getId(), type).stream().map(this::toResponse).toList();
    }

    public PersonalTaskResponse create(String authorizationHeader, PersonalTaskCreateRequest request) {
        AppUser user = authService.requireUser(authorizationHeader);
        PersonalTask task = repository.save(user.getId(), request.getType(), request.getContent(), now());
        return toResponse(task);
    }

    public PersonalTaskResponse update(String authorizationHeader, Long taskId, PersonalTaskUpdateRequest request) {
        AppUser user = authService.requireUser(authorizationHeader);
        PersonalTask existing = repository.findById(user.getId(), taskId)
                .orElseThrow(() -> new PersonalTaskNotFoundException(taskId));
        boolean pinned = request.getPinned() != null ? request.getPinned() : existing.isPinned();
        PersonalTask task = repository.update(user.getId(), taskId, request.getContent(), request.getCompleted(), pinned, now());
        if (task == null) throw new PersonalTaskNotFoundException(taskId);
        return toResponse(task);
    }

    @Transactional
    public void reorder(String authorizationHeader, PersonalTaskReorderRequest request) {
        AppUser user = authService.requireUser(authorizationHeader);
        List<PersonalTask> currentTasks = repository.findAll(user.getId(), request.getType());
        Set<Long> currentIds = currentTasks.stream().map(PersonalTask::getId).collect(java.util.stream.Collectors.toSet());
        Set<Long> requestedIds = Set.copyOf(request.getTaskIds());
        if (requestedIds.size() != request.getTaskIds().size() || !requestedIds.equals(currentIds)) {
            throw new IllegalArgumentException("taskIds must contain every active task in this list exactly once");
        }
        LocalDateTime timestamp = now();
        for (int index = 0; index < request.getTaskIds().size(); index += 1) {
            repository.updateSortOrder(user.getId(), request.getTaskIds().get(index), index + 1, timestamp);
        }
    }

    public void delete(String authorizationHeader, Long taskId) {
        AppUser user = authService.requireUser(authorizationHeader);
        if (!repository.softDelete(user.getId(), taskId, now())) throw new PersonalTaskNotFoundException(taskId);
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private PersonalTaskResponse toResponse(PersonalTask task) {
        PersonalTaskResponse response = new PersonalTaskResponse();
        response.setId(task.getId());
        response.setType(task.getTaskType());
        response.setContent(task.getContent());
        response.setCompleted(task.isCompleted());
        response.setPinned(task.isPinned());
        response.setSortOrder(task.getSortOrder());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        return response;
    }
}
