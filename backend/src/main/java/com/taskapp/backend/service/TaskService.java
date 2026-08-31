package com.taskapp.backend.service;

import com.taskapp.backend.dto.PhaseRequest;
import com.taskapp.backend.dto.PhaseResponse;
import com.taskapp.backend.dto.TaskCreateRequest;
import com.taskapp.backend.dto.TaskNoteCreateRequest;
import com.taskapp.backend.dto.TaskNoteResponse;
import com.taskapp.backend.dto.TaskPinRequest;
import com.taskapp.backend.dto.TaskResponse;
import com.taskapp.backend.dto.TaskShareRequest;
import com.taskapp.backend.dto.TaskShareResponse;
import com.taskapp.backend.dto.TaskUpdateRequest;
import com.taskapp.backend.dto.TaskVersionResponse;
import com.taskapp.backend.dto.TaskConflictResponse;
import com.taskapp.backend.dto.TaskConflictDraftResponse;
import com.taskapp.backend.dto.UserResponse;
import com.taskapp.backend.exception.AuthorizationException;
import com.taskapp.backend.exception.TaskNoteNotFoundException;
import com.taskapp.backend.exception.TaskNotFoundException;
import com.taskapp.backend.exception.TaskConflictException;
import com.taskapp.backend.exception.UserNotFoundException;
import com.taskapp.backend.model.AppUser;
import com.taskapp.backend.model.PhaseStatus;
import com.taskapp.backend.model.ProjectPriority;
import com.taskapp.backend.model.SharePermission;
import com.taskapp.backend.model.Task;
import com.taskapp.backend.model.TaskKnowledge;
import com.taskapp.backend.model.TaskNote;
import com.taskapp.backend.model.TaskPhase;
import com.taskapp.backend.model.TaskShare;
import com.taskapp.backend.repository.TaskKnowledgeRepository;
import com.taskapp.backend.repository.TaskNoteRepository;
import com.taskapp.backend.repository.TaskPhaseRepository;
import com.taskapp.backend.repository.TaskPinRepository;
import com.taskapp.backend.repository.TaskRepository;
import com.taskapp.backend.repository.TaskShareRepository;
import com.taskapp.backend.repository.TaskVersionRepository;
import com.taskapp.backend.repository.TaskConflictDraftRepository;
import com.taskapp.backend.repository.UserRepository;
import com.taskapp.backend.model.TaskVersion;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TaskService {

    private static final int DEFAULT_PHASE_COUNT = 1;

    private final TaskRepository taskRepository;
    private final TaskPhaseRepository taskPhaseRepository;
    private final TaskKnowledgeRepository taskKnowledgeRepository;
    private final TaskNoteRepository taskNoteRepository;
    private final TaskShareRepository taskShareRepository;
    private final TaskPinRepository taskPinRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final TaskVersionRepository taskVersionRepository;
    private final ObjectMapper objectMapper;
    private final TaskConflictDraftRepository taskConflictDraftRepository;

    public TaskService(
            TaskRepository taskRepository,
            TaskPhaseRepository taskPhaseRepository,
            TaskKnowledgeRepository taskKnowledgeRepository,
            TaskNoteRepository taskNoteRepository,
            TaskShareRepository taskShareRepository,
            TaskPinRepository taskPinRepository,
            UserRepository userRepository,
            AuthService authService,
            TaskVersionRepository taskVersionRepository,
            ObjectMapper objectMapper,
            TaskConflictDraftRepository taskConflictDraftRepository
    ) {
        this.taskRepository = taskRepository;
        this.taskPhaseRepository = taskPhaseRepository;
        this.taskKnowledgeRepository = taskKnowledgeRepository;
        this.taskNoteRepository = taskNoteRepository;
        this.taskShareRepository = taskShareRepository;
        this.taskPinRepository = taskPinRepository;
        this.userRepository = userRepository;
        this.authService = authService;
        this.taskVersionRepository = taskVersionRepository;
        this.objectMapper = objectMapper;
        this.taskConflictDraftRepository = taskConflictDraftRepository;
    }

    public List<TaskResponse> getAllTasks(String authorizationHeader, String keyword, String sortBy, String order, boolean archived) {
        AppUser currentUser = authService.requireUser(authorizationHeader);
        List<Task> tasks = taskRepository.findAllForUser(currentUser.getId(), keyword, sortBy, order, archived);
        return buildTaskResponses(tasks, currentUser);
    }

    private List<TaskResponse> buildTaskResponses(List<Task> tasks, AppUser currentUser) {
        List<Long> taskIds = tasks.stream().map(Task::getId).toList();
        Map<Long, List<TaskPhase>> phaseMap = taskPhaseRepository.findByTaskIds(taskIds);
        Map<Long, TaskKnowledge> knowledgeMap = taskKnowledgeRepository.findByTaskIds(taskIds);
        Map<Long, List<TaskNote>> noteMap = taskNoteRepository.findByTaskIds(taskIds);
        Map<Long, AppUser> ownerMap = userRepository.findMapByIds(tasks.stream().map(Task::getOwnerUserId).distinct().toList());

        return tasks.stream()
                .map(task -> {
                    List<TaskPhase> phases = phaseMap.get(task.getId());
                    if (phases == null || phases.isEmpty()) {
                        phases = backfillLegacyPhases(task);
                    }
                    return toResponse(task, phases, knowledgeMap.get(task.getId()), noteMap.get(task.getId()), currentUser, ownerMap.get(task.getOwnerUserId()));
                })
                .toList();
    }

    public List<TaskResponse> getAllTasks(String authorizationHeader, String keyword, String sortBy, String order) {
        AppUser currentUser = authService.requireUser(authorizationHeader);
        List<Task> tasks = taskRepository.findAllForUser(currentUser.getId(), keyword, sortBy, order);
        return buildTaskResponses(tasks, currentUser);
    }

    public TaskResponse getTaskById(String authorizationHeader, Long id) {
        AppUser currentUser = authService.requireUser(authorizationHeader);
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        requireCanView(currentUser, task);

        List<TaskPhase> phases = taskPhaseRepository.findByTaskId(id);
        if (phases.isEmpty()) {
            phases = backfillLegacyPhases(task);
        }
        TaskKnowledge knowledge = taskKnowledgeRepository.findByTaskId(id).orElse(null);
        List<TaskNote> notes = taskNoteRepository.findByTaskId(id);
        AppUser owner = userRepository.findById(task.getOwnerUserId()).orElse(null);
        return toResponse(task, phases, knowledge, notes, currentUser, owner);
    }

    public TaskResponse createTask(String authorizationHeader, TaskCreateRequest request) {
        AppUser currentUser = authService.requireUser(authorizationHeader);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        List<TaskPhase> normalizedPhases = normalizePhases(request.getPhases());

        Task task = new Task();
        task.setTaskTitle(request.getTaskTitle().trim());
        task.setTaskDescription(normalizeDescription(request.getTaskDescription()));
        task.setOwnerUserId(currentUser.getId());
        task.setPriority(resolvePriority(request.getPriority()));
        applyLegacyPhaseColumns(task, normalizedPhases);
        task.setOverallProgress(calculateOverallProgress(normalizedPhases));
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        task.setRevision(1);

        Task savedTask = taskRepository.save(task);
        taskPhaseRepository.insertAll(savedTask.getId(), normalizedPhases, now);
        taskKnowledgeRepository.upsert(
                savedTask.getId(),
                request.getRecentDecisions(),
                request.getRecentExperiments(),
                request.getKnowledgeHighlights(),
                now
        );

        List<TaskPhase> savedPhases = taskPhaseRepository.findByTaskId(savedTask.getId());
        TaskKnowledge knowledge = taskKnowledgeRepository.findByTaskId(savedTask.getId()).orElse(null);
        List<TaskNote> notes = taskNoteRepository.findByTaskId(savedTask.getId());
        return toResponse(savedTask, savedPhases, knowledge, notes, currentUser, currentUser);
    }

    @Transactional(noRollbackFor = TaskConflictException.class)
    public TaskResponse updateTask(String authorizationHeader, Long id, TaskUpdateRequest request) {
        AppUser currentUser = authService.requireUser(authorizationHeader);
        Task existingTask = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        requireCanEdit(currentUser, existingTask);
        if (request.getExpectedRevision() == null || request.getExpectedRevision() != existingTask.getRevision()) {
            throw conflict(existingTask, currentUser, request);
        }
        snapshot(existingTask, currentUser, "project update");

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        List<TaskPhase> normalizedPhases = normalizePhases(request.getPhases());

        existingTask.setTaskTitle(request.getTaskTitle().trim());
        existingTask.setTaskDescription(normalizeDescription(request.getTaskDescription()));
        existingTask.setPriority(resolvePriority(request.getPriority()));
        applyLegacyPhaseColumns(existingTask, normalizedPhases);
        existingTask.setOverallProgress(calculateOverallProgress(normalizedPhases));
        existingTask.setUpdatedAt(now);

        if (!taskRepository.update(existingTask, request.getExpectedRevision())) {
            throw conflict(taskRepository.findById(id).orElse(existingTask), currentUser, request);
        }
        Task updatedTask = existingTask;
        taskRepository.setDemo(id, false);
        updatedTask.setDemo(false);
        taskPhaseRepository.replaceAll(id, normalizedPhases, now);
        taskKnowledgeRepository.upsert(
                id,
                request.getRecentDecisions(),
                request.getRecentExperiments(),
                request.getKnowledgeHighlights(),
                now
        );

        List<TaskPhase> savedPhases = taskPhaseRepository.findByTaskId(id);
        TaskKnowledge knowledge = taskKnowledgeRepository.findByTaskId(id).orElse(null);
        List<TaskNote> notes = taskNoteRepository.findByTaskId(id);
        AppUser owner = userRepository.findById(updatedTask.getOwnerUserId()).orElse(null);
        return toResponse(updatedTask, savedPhases, knowledge, notes, currentUser, owner);
    }

    public List<TaskVersionResponse> getTaskVersions(String authorizationHeader, Long id) {
        AppUser currentUser = authService.requireUser(authorizationHeader);
        Task task = taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
        requireCanView(currentUser, task);
        return taskVersionRepository.findByTaskId(id).stream().map(this::toVersionResponse).toList();
    }

    public TaskConflictDraftResponse getConflictDraft(String authorizationHeader, Long id) {
        AppUser currentUser = authService.requireUser(authorizationHeader);
        Task task = taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
        requireCanEdit(currentUser, task);
        return taskConflictDraftRepository.findLatestActive(id, currentUser.getId()).map(draft -> {
            try {
                TaskConflictDraftResponse response = new TaskConflictDraftResponse();
                response.setId(draft.id()); response.setCreatedAt(draft.createdAt()); response.setPayload(objectMapper.readValue(draft.payloadJson(), TaskUpdateRequest.class));
                return response;
            } catch (JsonProcessingException ex) { throw new IllegalArgumentException("Draft data could not be read"); }
        }).orElse(null);
    }

    public void resolveConflictDraft(String authorizationHeader, Long id, Long draftId) {
        AppUser currentUser = authService.requireUser(authorizationHeader);
        Task task = taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
        requireCanEdit(currentUser, task);
        taskConflictDraftRepository.resolve(draftId, currentUser.getId(), LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    @Transactional
    public TaskResponse restoreTaskVersion(String authorizationHeader, Long id, Long versionId) {
        AppUser currentUser = authService.requireUser(authorizationHeader);
        Task task = taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
        requireCanEdit(currentUser, task);
        TaskVersion version = taskVersionRepository.findByIdAndTaskId(versionId, id).orElseThrow(() -> new IllegalArgumentException("Version not found"));
        try {
            TaskResponse snapshot = objectMapper.readValue(version.getSnapshotJson(), TaskResponse.class);
            snapshot(task, currentUser, "before restore");
            task.setTaskTitle(snapshot.getTaskTitle()); task.setTaskDescription(snapshot.getTaskDescription());
            task.setPriority(ProjectPriority.valueOf(snapshot.getPriority()));
            List<TaskPhase> phases = snapshot.getPhases().stream().map(this::fromPhaseResponse).toList();
            applyLegacyPhaseColumns(task, phases); task.setOverallProgress(calculateOverallProgress(phases));
            task.setUpdatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
            if (!taskRepository.update(task, task.getRevision())) throw new TaskConflictException();
            taskPhaseRepository.replaceAll(id, phases, task.getUpdatedAt());
            taskKnowledgeRepository.upsert(id, snapshot.getRecentDecisions(), snapshot.getRecentExperiments(), snapshot.getKnowledgeHighlights(), task.getUpdatedAt());
            taskNoteRepository.replaceAll(id, snapshot.getNotes(), task.getUpdatedAt());
            List<TaskPhase> restoredPhases = taskPhaseRepository.findByTaskId(id);
            TaskKnowledge knowledge = taskKnowledgeRepository.findByTaskId(id).orElse(null);
            return toResponse(task, restoredPhases, knowledge, taskNoteRepository.findByTaskId(id), currentUser, userRepository.findById(task.getOwnerUserId()).orElse(null));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Version data could not be restored");
        }
    }

    public void deleteTask(String authorizationHeader, Long id) {
        AppUser currentUser = authService.requireUser(authorizationHeader);
        Task existingTask = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        requireOwner(currentUser, existingTask);

        taskRepository.deleteById(existingTask.getId());
    }

    public void setTaskArchived(String authorizationHeader, Long id, boolean archived) {
        AppUser currentUser = authService.requireUser(authorizationHeader);
        Task task = taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
        requireOwner(currentUser, task);
        taskRepository.setDemo(id, false);
        taskRepository.setArchived(id, archived, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    public void setTaskPinned(String authorizationHeader, Long taskId, TaskPinRequest request) {
        AppUser currentUser = authService.requireUser(authorizationHeader);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        requireCanView(currentUser, task);
        taskRepository.setDemo(taskId, false);
        taskPinRepository.setPinned(taskId, currentUser.getId(), request.getPinned(), LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    public TaskNoteResponse addTaskNote(String authorizationHeader, Long taskId, TaskNoteCreateRequest request) {
        AppUser currentUser = authService.requireUser(authorizationHeader);
        Task existingTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        requireCanEdit(currentUser, existingTask);
        snapshot(existingTask, currentUser, "knowledge item created");
        taskRepository.setDemo(taskId, false);

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        TaskNote saved = taskNoteRepository.save(
                existingTask.getId(),
                request.getNoteType(),
                request.getNoteContent(),
                now
        );
        return toNoteResponse(saved);
    }

    public TaskNoteResponse updateTaskNote(String authorizationHeader, Long taskId, Long noteId, TaskNoteCreateRequest request) {
        AppUser currentUser = authService.requireUser(authorizationHeader);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        requireCanEdit(currentUser, task);
        snapshot(task, currentUser, "knowledge item updated");
        taskRepository.setDemo(taskId, false);

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        TaskNote updated = taskNoteRepository.update(
                noteId,
                taskId,
                request.getNoteType(),
                request.getNoteContent(),
                now
        );
        if (updated == null) {
            throw new TaskNoteNotFoundException(taskId, noteId);
        }
        return toNoteResponse(updated);
    }

    public void deleteTaskNote(String authorizationHeader, Long taskId, Long noteId) {
        AppUser currentUser = authService.requireUser(authorizationHeader);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        requireCanEdit(currentUser, task);
        snapshot(task, currentUser, "knowledge item deleted");
        taskRepository.setDemo(taskId, false);

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        boolean deleted = taskNoteRepository.softDelete(noteId, taskId, now);
        if (!deleted) {
            throw new TaskNoteNotFoundException(taskId, noteId);
        }
    }

    public List<TaskShareResponse> getTaskShares(String authorizationHeader, Long taskId) {
        AppUser currentUser = authService.requireUser(authorizationHeader);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        requireOwner(currentUser, task);
        return buildShareResponses(taskShareRepository.findByTaskId(taskId));
    }

    public TaskShareResponse shareTask(String authorizationHeader, Long taskId, TaskShareRequest request) {
        AppUser currentUser = authService.requireUser(authorizationHeader);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        requireOwner(currentUser, task);

        AppUser targetUser = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UserNotFoundException(request.getUsername()));
        if (targetUser.getId().equals(currentUser.getId())) {
            throw new AuthorizationException("Cannot share a task with yourself");
        }

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        TaskShare share = taskShareRepository.upsert(taskId, targetUser.getId(), request.getPermission(), now);
        return toShareResponse(share, targetUser);
    }

    public TaskShareResponse updateTaskShare(String authorizationHeader, Long taskId, Long shareId, TaskShareRequest request) {
        AppUser currentUser = authService.requireUser(authorizationHeader);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        requireOwner(currentUser, task);

        TaskShare existing = taskShareRepository.findById(shareId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        if (!existing.getTaskId().equals(taskId)) {
            throw new TaskNotFoundException(taskId);
        }

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        TaskShare share = taskShareRepository.upsert(taskId, existing.getSharedWithUserId(), request.getPermission(), now);
        AppUser targetUser = userRepository.findById(share.getSharedWithUserId()).orElse(null);
        return toShareResponse(share, targetUser);
    }

    public void deleteTaskShare(String authorizationHeader, Long taskId, Long shareId) {
        AppUser currentUser = authService.requireUser(authorizationHeader);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        requireOwner(currentUser, task);
        boolean deleted = taskShareRepository.delete(taskId, shareId);
        if (!deleted) {
            throw new TaskNotFoundException(taskId);
        }
    }

    private List<TaskPhase> normalizePhases(List<PhaseRequest> phaseRequests) {
        List<TaskPhase> phases = new ArrayList<>();

        if (phaseRequests != null) {
            for (PhaseRequest request : phaseRequests) {
                if (request == null) {
                    continue;
                }

                String phaseName = request.getPhaseName() == null ? "" : request.getPhaseName().trim();
                if (phaseName.isEmpty()) {
                    continue;
                }

                TaskPhase phase = new TaskPhase();
                phase.setPhaseKey(normalizePhaseKey(request.getPhaseKey(), phases.size() + 1));
                phase.setParentPhaseKey(normalizeParentPhaseKey(request.getParentPhaseKey()));
                phase.setPhaseName(phaseName);
                phase.setPhaseDescription(normalizePhaseDescription(request.getPhaseDescription()));
                phase.setPhaseStatus(request.getPhaseStatus() == null ? PhaseStatus.TODO : request.getPhaseStatus());
                phases.add(phase);
            }
        }

        while (phases.size() < DEFAULT_PHASE_COUNT) {
            int index = phases.size() + 1;
            TaskPhase phase = new TaskPhase();
            phase.setPhaseKey("phase-" + index);
            phase.setParentPhaseKey(null);
            phase.setPhaseName("阶段" + index);
            phase.setPhaseDescription(null);
            phase.setPhaseStatus(PhaseStatus.TODO);
            phases.add(phase);
        }

        validateAndApplySiblingSort(phases);

        return phases;
    }

    private String normalizePhaseKey(String phaseKey, int fallbackIndex) {
        if (phaseKey == null || phaseKey.isBlank()) {
            return "phase-" + fallbackIndex;
        }
        return phaseKey.trim();
    }

    private String normalizeParentPhaseKey(String parentPhaseKey) {
        if (parentPhaseKey == null) {
            return null;
        }
        String trimmed = parentPhaseKey.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateAndApplySiblingSort(List<TaskPhase> phases) {
        Set<String> phaseKeys = new HashSet<>();
        Map<String, String> parentByPhaseKey = new HashMap<>();

        for (TaskPhase phase : phases) {
            String phaseKey = phase.getPhaseKey();
            if (!phaseKeys.add(phaseKey)) {
                throw new IllegalArgumentException("phaseKey must be unique within a task");
            }
            parentByPhaseKey.put(phaseKey, phase.getParentPhaseKey());
        }

        for (TaskPhase phase : phases) {
            String phaseKey = phase.getPhaseKey();
            String parentPhaseKey = phase.getParentPhaseKey();
            if (parentPhaseKey != null && !phaseKeys.contains(parentPhaseKey)) {
                throw new IllegalArgumentException("parentPhaseKey must reference another phase in the same task");
            }
            if (phaseKey.equals(parentPhaseKey)) {
                throw new IllegalArgumentException("phase parent cannot reference itself");
            }

            Set<String> path = new HashSet<>();
            String currentPhaseKey = phaseKey;
            while (currentPhaseKey != null) {
                if (!path.add(currentPhaseKey)) {
                    throw new IllegalArgumentException("phase tree cannot contain cycles");
                }
                currentPhaseKey = parentByPhaseKey.get(currentPhaseKey);
            }
        }

        for (int i = 0; i < phases.size(); i++) {
            phases.get(i).setSortOrder(i + 1);
        }
    }

    private void applyLegacyPhaseColumns(Task task, List<TaskPhase> phases) {
        task.setPhase1Status(getPhaseStatusOrDefault(phases, 0));
        task.setPhase2Status(getPhaseStatusOrDefault(phases, 1));
        task.setPhase3Status(getPhaseStatusOrDefault(phases, 2));
    }

    private PhaseStatus getPhaseStatusOrDefault(List<TaskPhase> phases, int index) {
        if (index >= phases.size()) {
            return PhaseStatus.TODO;
        }
        return phases.get(index).getPhaseStatus();
    }

    private double calculateOverallProgress(List<TaskPhase> phases) {
        if (phases == null || phases.isEmpty()) {
            return 0.0;
        }

        double total = phases.stream()
                .mapToInt(phase -> phase.getPhaseStatus().getScore())
                .sum();

        double raw = total / phases.size();
        return BigDecimal.valueOf(raw)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private void requireCanView(AppUser user, Task task) {
        if (isOwner(user, task) || taskShareRepository.findByTaskIdAndUserId(task.getId(), user.getId()).isPresent()) {
            return;
        }
        throw new TaskNotFoundException(task.getId());
    }

    private void requireCanEdit(AppUser user, Task task) {
        if (isOwner(user, task)) {
            return;
        }
        boolean canEdit = taskShareRepository.findByTaskIdAndUserId(task.getId(), user.getId())
                .map(share -> share.getPermission() == SharePermission.EDIT)
                .orElse(false);
        if (!canEdit) {
            throw new AuthorizationException("You do not have permission to edit this task");
        }
    }

    private void requireOwner(AppUser user, Task task) {
        if (!isOwner(user, task)) {
            throw new AuthorizationException("Only the owner can perform this action");
        }
    }

    private boolean isOwner(AppUser user, Task task) {
        return task.getOwnerUserId() != null && task.getOwnerUserId().equals(user.getId());
    }

    private String resolveAccessLevel(AppUser user, Task task) {
        if (isOwner(user, task)) {
            return "OWNER";
        }
        return taskShareRepository.findByTaskIdAndUserId(task.getId(), user.getId())
                .map(share -> share.getPermission().name())
                .orElse("NONE");
    }

    private TaskResponse toResponse(Task task, List<TaskPhase> phases, TaskKnowledge knowledge, List<TaskNote> notes, AppUser currentUser, AppUser owner) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setTaskTitle(task.getTaskTitle());
        response.setTaskDescription(task.getTaskDescription());
        response.setRecentDecisions(knowledge == null ? null : knowledge.getRecentDecisions());
        response.setRecentExperiments(knowledge == null ? null : knowledge.getRecentExperiments());
        response.setKnowledgeHighlights(knowledge == null ? null : knowledge.getKnowledgeHighlights());
        response.setArchived(task.isArchived());
        response.setArchivedAt(task.getArchivedAt());
        response.setDemo(task.isDemo());
        response.setPriority(task.getPriority() == null ? ProjectPriority.MEDIUM.name() : task.getPriority().name());
        response.setOwnerUserId(task.getOwnerUserId());
        response.setOwnerUsername(owner == null ? null : owner.getUsername());
        response.setOwnedByCurrentUser(isOwner(currentUser, task));
        response.setSharedWithCurrentUser(!isOwner(currentUser, task));
        response.setAccessLevel(resolveAccessLevel(currentUser, task));
        response.setPinned(task.isPinned());
        response.setPhases(phases.stream().map(this::toPhaseResponse).toList());
        response.setNotes((notes == null ? List.<TaskNote>of() : notes).stream().map(this::toNoteResponse).toList());
        response.setOverallProgress(task.getOverallProgress());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        response.setRevision(task.getRevision());
        return response;
    }

    private void snapshot(Task task, AppUser changedBy, String reason) {
        try {
            AppUser owner = userRepository.findById(task.getOwnerUserId()).orElse(null);
            TaskResponse current = toResponse(task, taskPhaseRepository.findByTaskId(task.getId()), taskKnowledgeRepository.findByTaskId(task.getId()).orElse(null), taskNoteRepository.findByTaskId(task.getId()), changedBy, owner);
            taskVersionRepository.save(task.getId(), task.getRevision(), objectMapper.writeValueAsString(current), reason, changedBy.getId(), LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not create a safety snapshot", ex);
        }
    }

    private TaskVersionResponse toVersionResponse(TaskVersion version) {
        TaskVersionResponse response = new TaskVersionResponse();
        response.setId(version.getId()); response.setRevision(version.getRevision()); response.setChangeReason(version.getChangeReason());
        response.setChangedBy(version.getChangedByUsername()); response.setCreatedAt(version.getCreatedAt());
        return response;
    }

    private TaskConflictException conflict(Task task, AppUser currentUser, TaskUpdateRequest request) {
        try {
            Long draftId = taskConflictDraftRepository.save(task.getId(), currentUser.getId(), request.getExpectedRevision() == null ? 0 : request.getExpectedRevision(), objectMapper.writeValueAsString(request), LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
            AppUser owner = userRepository.findById(task.getOwnerUserId()).orElse(null);
            TaskConflictResponse response = new TaskConflictResponse();
            response.setDraftId(draftId);
            response.setLatestTask(toResponse(task, taskPhaseRepository.findByTaskId(task.getId()), taskKnowledgeRepository.findByTaskId(task.getId()).orElse(null), taskNoteRepository.findByTaskId(task.getId()), currentUser, owner));
            return new TaskConflictException(response);
        } catch (JsonProcessingException ex) {
            return new TaskConflictException();
        }
    }

    private TaskPhase fromPhaseResponse(PhaseResponse response) {
        TaskPhase phase = new TaskPhase();
        phase.setPhaseKey(response.getPhaseKey()); phase.setParentPhaseKey(response.getParentPhaseKey()); phase.setPhaseName(response.getPhaseName());
        phase.setPhaseDescription(response.getPhaseDescription()); phase.setPhaseStatus(response.getPhaseStatus()); phase.setSortOrder(response.getSortOrder());
        return phase;
    }

    private List<TaskShareResponse> buildShareResponses(List<TaskShare> shares) {
        Map<Long, AppUser> userMap = userRepository.findMapByIds(shares.stream().map(TaskShare::getSharedWithUserId).toList());
        return shares.stream()
                .map(share -> toShareResponse(share, userMap.get(share.getSharedWithUserId())))
                .toList();
    }

    private TaskShareResponse toShareResponse(TaskShare share, AppUser user) {
        TaskShareResponse response = new TaskShareResponse();
        response.setId(share.getId());
        response.setTaskId(share.getTaskId());
        response.setPermission(share.getPermission());
        response.setSharedWith(user == null ? null : toUserResponse(user));
        return response;
    }

    private UserResponse toUserResponse(AppUser user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setDisplayName(user.getDisplayName());
        return response;
    }

    private PhaseResponse toPhaseResponse(TaskPhase phase) {
        PhaseResponse response = new PhaseResponse();
        response.setId(phase.getId());
        response.setPhaseKey(phase.getPhaseKey());
        response.setParentPhaseKey(phase.getParentPhaseKey());
        response.setPhaseName(phase.getPhaseName());
        response.setPhaseDescription(phase.getPhaseDescription());
        response.setPhaseStatus(phase.getPhaseStatus());
        response.setSortOrder(phase.getSortOrder());
        return response;
    }

    private TaskNoteResponse toNoteResponse(TaskNote note) {
        TaskNoteResponse response = new TaskNoteResponse();
        response.setId(note.getId());
        response.setNoteType(note.getNoteType());
        response.setNoteContent(note.getNoteContent());
        response.setCreatedAt(note.getCreatedAt());
        response.setUpdatedAt(note.getUpdatedAt());
        return response;
    }

    private List<TaskPhase> backfillLegacyPhases(Task task) {
        List<TaskPhase> phases = List.of(
                buildLegacyPhase(task.getId(), "阶段1", task.getPhase1Status(), 1),
                buildLegacyPhase(task.getId(), "阶段2", task.getPhase2Status(), 2),
                buildLegacyPhase(task.getId(), "阶段3", task.getPhase3Status(), 3)
        );

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        taskPhaseRepository.insertAll(task.getId(), phases, now);
        return taskPhaseRepository.findByTaskId(task.getId());
    }

    private TaskPhase buildLegacyPhase(Long taskId, String name, PhaseStatus status, int sortOrder) {
        TaskPhase phase = new TaskPhase();
        phase.setTaskId(taskId);
        phase.setPhaseKey("phase-" + sortOrder);
        phase.setParentPhaseKey(null);
        phase.setPhaseName(name);
        phase.setPhaseDescription(null);
        phase.setPhaseStatus(status == null ? PhaseStatus.TODO : status);
        phase.setSortOrder(sortOrder);
        return phase;
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ProjectPriority resolvePriority(ProjectPriority requestPriority) {
        if (requestPriority == null) {
            return ProjectPriority.MEDIUM;
        }
        return requestPriority;
    }

    private String normalizePhaseDescription(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
