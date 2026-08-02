package com.taskapp.backend.dto;

import com.taskapp.backend.model.PhaseStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record TaskAiExportResponse(
        String schemaVersion,
        Instant exportedAt,
        Project project
) {

    public record Project(
            Long id,
            String title,
            String description,
            String priority,
            double overallProgress,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<Phase> phases,
            Knowledge knowledge
    ) {
    }

    public record Phase(
            String key,
            String name,
            String description,
            PhaseStatus status,
            int sortOrder,
            List<Phase> children
    ) {
    }

    public record Knowledge(
            KnowledgeSection recentDecisions,
            KnowledgeSection recentExperiments,
            KnowledgeSection knowledgeHighlights,
            KnowledgeSection aiSuggestions
    ) {
    }

    public record KnowledgeSection(
            String summary,
            List<KnowledgeEntry> entries
    ) {
    }

    public record KnowledgeEntry(
            String content,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}
