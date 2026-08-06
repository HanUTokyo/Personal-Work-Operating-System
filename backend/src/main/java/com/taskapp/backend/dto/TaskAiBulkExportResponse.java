package com.taskapp.backend.dto;

import java.time.Instant;
import java.util.List;

public record TaskAiBulkExportResponse(
        String schemaVersion,
        Instant exportedAt,
        int projectCount,
        List<TaskAiExportResponse.Project> projects,
        List<PersonalTaskResponse> weeklyTasks,
        List<PersonalTaskResponse> longTermTasks,
        List<GlobalAiSuggestionResponse> aiSuggestions,
        List<GlobalAiSuggestionResponse> currentActionGoals,
        List<FlashNoteResponse> flashNotes
) {
}
