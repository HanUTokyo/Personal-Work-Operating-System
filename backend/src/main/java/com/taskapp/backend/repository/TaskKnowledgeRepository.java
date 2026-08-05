package com.taskapp.backend.repository;

import com.taskapp.backend.model.TaskKnowledge;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class TaskKnowledgeRepository {

    private static final DateTimeFormatter DB_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<TaskKnowledge> knowledgeRowMapper = (rs, rowNum) -> {
        TaskKnowledge knowledge = new TaskKnowledge();
        knowledge.setTaskId(rs.getLong("task_id"));
        knowledge.setRecentDecisions(rs.getString("recent_decisions"));
        knowledge.setRecentExperiments(rs.getString("recent_experiments"));
        knowledge.setKnowledgeHighlights(rs.getString("knowledge_highlights"));
        knowledge.setCurrentActionGoal(rs.getString("current_action_goal"));
        knowledge.setCreatedAt(parseDateTime(rs.getString("created_at")));
        knowledge.setUpdatedAt(parseDateTime(rs.getString("updated_at")));
        return knowledge;
    };

    public TaskKnowledgeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<TaskKnowledge> findByTaskId(Long taskId) {
        String sql = """
                SELECT task_id, recent_decisions, recent_experiments, knowledge_highlights, current_action_goal, created_at, updated_at
                FROM task_knowledge
                WHERE task_id = ?
                """;
        List<TaskKnowledge> rows = jdbcTemplate.query(sql, knowledgeRowMapper, taskId);
        return rows.stream().findFirst();
    }

    public Map<Long, TaskKnowledge> findByTaskIds(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Map.of();
        }

        String placeholders = String.join(",", taskIds.stream().map(id -> "?").toList());
        String sql = """
                SELECT task_id, recent_decisions, recent_experiments, knowledge_highlights, current_action_goal, created_at, updated_at
                FROM task_knowledge
                WHERE task_id IN (%s)
                """.formatted(placeholders);

        List<TaskKnowledge> rows = jdbcTemplate.query(sql, knowledgeRowMapper, taskIds.toArray());
        Map<Long, TaskKnowledge> result = new HashMap<>();
        for (TaskKnowledge row : rows) {
            result.put(row.getTaskId(), row);
        }
        return result;
    }

    public void upsert(
            Long taskId,
            String recentDecisions,
            String recentExperiments,
            String knowledgeHighlights,
            String currentActionGoal,
            LocalDateTime now
    ) {
        String normalizedDecisions = normalizeNote(recentDecisions);
        String normalizedExperiments = normalizeNote(recentExperiments);
        String normalizedHighlights = normalizeNote(knowledgeHighlights);
        String normalizedActionGoal = normalizeNote(currentActionGoal);

        int updatedRows = jdbcTemplate.update(
                """
                UPDATE task_knowledge
                SET recent_decisions = ?,
                    recent_experiments = ?,
                    knowledge_highlights = ?,
                    current_action_goal = ?,
                    updated_at = ?
                WHERE task_id = ?
                """,
                normalizedDecisions,
                normalizedExperiments,
                normalizedHighlights,
                normalizedActionGoal,
                formatDateTime(now),
                taskId
        );

        if (updatedRows > 0) {
            return;
        }

        jdbcTemplate.update(
                """
                INSERT INTO task_knowledge (
                    task_id, recent_decisions, recent_experiments, knowledge_highlights, current_action_goal, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                taskId,
                normalizedDecisions,
                normalizedExperiments,
                normalizedHighlights,
                normalizedActionGoal,
                formatDateTime(now),
                formatDateTime(now)
        );
    }

    private String normalizeNote(String note) {
        if (note == null) {
            return null;
        }
        String trimmed = note.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.truncatedTo(ChronoUnit.SECONDS).format(DB_DATE_TIME_FORMATTER);
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DB_DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            return LocalDateTime.parse(value);
        }
    }
}
