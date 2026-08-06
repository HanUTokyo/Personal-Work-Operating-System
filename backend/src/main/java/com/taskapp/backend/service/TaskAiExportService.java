package com.taskapp.backend.service;

import com.taskapp.backend.dto.PhaseResponse;
import com.taskapp.backend.dto.TaskAiBulkExportResponse;
import com.taskapp.backend.dto.TaskAiExportResponse;
import com.taskapp.backend.dto.TaskNoteResponse;
import com.taskapp.backend.dto.TaskResponse;
import com.taskapp.backend.model.NoteType;
import com.taskapp.backend.model.PersonalTaskType;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TaskAiExportService {

    private static final String PROJECT_SCHEMA_VERSION = "1.1";
    private static final String WORKSPACE_SCHEMA_VERSION = "1.3";

    private final TaskService taskService;
    private final PersonalTaskService personalTaskService;
    private final GlobalAiSuggestionService globalAiSuggestionService;
    private final FlashNoteService flashNoteService;

    public TaskAiExportService(
            TaskService taskService,
            PersonalTaskService personalTaskService,
            GlobalAiSuggestionService globalAiSuggestionService,
            FlashNoteService flashNoteService
    ) {
        this.taskService = taskService;
        this.personalTaskService = personalTaskService;
        this.globalAiSuggestionService = globalAiSuggestionService;
        this.flashNoteService = flashNoteService;
    }

    public TaskAiExportResponse exportTask(String authorizationHeader, Long taskId) {
        TaskResponse task = taskService.getTaskById(authorizationHeader, taskId);
        return new TaskAiExportResponse(PROJECT_SCHEMA_VERSION, Instant.now(), toExportProject(task));
    }

    public TaskAiBulkExportResponse exportAllTasks(String authorizationHeader) {
        List<TaskAiExportResponse.Project> projects = taskService
                .getAllTasks(authorizationHeader, null, "taskTitle", "asc")
                .stream()
                .map(this::toExportProject)
                .toList();
        return new TaskAiBulkExportResponse(
                WORKSPACE_SCHEMA_VERSION,
                Instant.now(),
                projects.size(),
                projects,
                personalTaskService.getAll(authorizationHeader, PersonalTaskType.WEEKLY),
                personalTaskService.getAll(authorizationHeader, PersonalTaskType.LONG_TERM),
                globalAiSuggestionService.getAll(authorizationHeader),
                globalAiSuggestionService.getAll(authorizationHeader, "ACTION_GOAL"),
                flashNoteService.getAllFlashNotes(authorizationHeader)
        );
    }

    private TaskAiExportResponse.Project toExportProject(TaskResponse task) {
        return new TaskAiExportResponse.Project(
                task.getId(),
                task.getTaskTitle(),
                task.getTaskDescription(),
                task.getPriority(),
                task.getOverallProgress(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                buildPhaseTree(task.getPhases()),
                buildKnowledge(task)
        );
    }

    private List<TaskAiExportResponse.Phase> buildPhaseTree(List<PhaseResponse> phases) {
        if (phases == null || phases.isEmpty()) {
            return List.of();
        }

        List<PhaseResponse> orderedPhases = phases.stream()
                .sorted(Comparator.comparingInt(PhaseResponse::getSortOrder))
                .toList();
        Map<String, PhaseNode> nodesByKey = new LinkedHashMap<>();
        for (PhaseResponse phase : orderedPhases) {
            nodesByKey.put(phase.getPhaseKey(), new PhaseNode(phase));
        }

        List<PhaseNode> roots = new ArrayList<>();
        for (PhaseResponse phase : orderedPhases) {
            PhaseNode node = nodesByKey.get(phase.getPhaseKey());
            PhaseNode parent = nodesByKey.get(phase.getParentPhaseKey());
            if (parent == null || parent == node) {
                roots.add(node);
            } else {
                parent.children.add(node);
            }
        }

        return roots.stream().map(this::toExportPhase).toList();
    }

    private TaskAiExportResponse.Phase toExportPhase(PhaseNode node) {
        PhaseResponse phase = node.phase;
        return new TaskAiExportResponse.Phase(
                phase.getPhaseKey(),
                phase.getPhaseName(),
                phase.getPhaseDescription(),
                phase.getPhaseStatus(),
                phase.getSortOrder(),
                node.children.stream().map(this::toExportPhase).toList()
        );
    }

    private TaskAiExportResponse.Knowledge buildKnowledge(TaskResponse task) {
        List<TaskNoteResponse> notes = task.getNotes() == null ? List.of() : task.getNotes();
        return new TaskAiExportResponse.Knowledge(
                buildKnowledgeSection(task.getRecentDecisions(), notes, NoteType.RECENT_DECISIONS),
                buildKnowledgeSection(task.getRecentExperiments(), notes, NoteType.RECENT_EXPERIMENTS),
                buildKnowledgeSection(task.getKnowledgeHighlights(), notes, NoteType.KNOWLEDGE_HIGHLIGHTS),
                buildKnowledgeSection(null, notes, NoteType.AI_SUGGESTIONS)
        );
    }

    private TaskAiExportResponse.KnowledgeSection buildKnowledgeSection(
            String summary,
            List<TaskNoteResponse> notes,
            NoteType noteType
    ) {
        List<TaskAiExportResponse.KnowledgeEntry> entries = notes.stream()
                .filter(note -> note.getNoteType() == noteType)
                .map(note -> new TaskAiExportResponse.KnowledgeEntry(
                        note.getNoteContent(),
                        note.getCreatedAt(),
                        note.getUpdatedAt()
                ))
                .toList();
        return new TaskAiExportResponse.KnowledgeSection(summary, entries);
    }

    private static final class PhaseNode {
        private final PhaseResponse phase;
        private final List<PhaseNode> children = new ArrayList<>();

        private PhaseNode(PhaseResponse phase) {
            this.phase = phase;
        }
    }
}
