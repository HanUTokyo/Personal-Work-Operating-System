package com.taskapp.backend.repository;

import com.taskapp.backend.model.NoteType;
import com.taskapp.backend.model.TaskNote;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.taskapp.backend.dto.TaskNoteResponse;

@Repository
public class TaskNoteRepository {

    private static final DateTimeFormatter DB_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<TaskNote> noteRowMapper = (rs, rowNum) -> {
        TaskNote note = new TaskNote();
        note.setId(rs.getLong("id"));
        note.setTaskId(rs.getLong("task_id"));
        note.setNoteType(NoteType.valueOf(rs.getString("note_type")));
        note.setNoteContent(rs.getString("note_content"));
        note.setCreatedAt(parseDateTime(rs.getString("created_at")));
        note.setUpdatedAt(parseDateTime(rs.getString("updated_at")));
        return note;
    };

    public TaskNoteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TaskNote> findByTaskId(Long taskId) {
        String sql = """
                SELECT id, task_id, note_type, note_content, created_at, updated_at
                FROM task_notes
                WHERE task_id = ? AND is_deleted = 0
                ORDER BY created_at DESC, id DESC
                """;
        return jdbcTemplate.query(sql, noteRowMapper, taskId);
    }

    public Map<Long, List<TaskNote>> findByTaskIds(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String placeholders = String.join(",", Collections.nCopies(taskIds.size(), "?"));
        String sql = """
                SELECT id, task_id, note_type, note_content, created_at, updated_at
                FROM task_notes
                WHERE task_id IN (%s) AND is_deleted = 0
                ORDER BY task_id ASC, created_at DESC, id DESC
                """.formatted(placeholders);

        List<TaskNote> allNotes = jdbcTemplate.query(sql, noteRowMapper, taskIds.toArray());
        Map<Long, List<TaskNote>> noteMap = new HashMap<>();
        for (TaskNote note : allNotes) {
            noteMap.computeIfAbsent(note.getTaskId(), ignored -> new ArrayList<>()).add(note);
        }
        return noteMap;
    }

    public TaskNote save(Long taskId, NoteType noteType, String noteContent, LocalDateTime now) {
        String sql = """
                INSERT INTO task_notes (task_id, note_type, note_content, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(
                sql,
                taskId,
                noteType.name(),
                normalizeText(noteContent),
                formatDateTime(now),
                formatDateTime(now)
        );

        List<TaskNote> rows = jdbcTemplate.query(
                """
                SELECT id, task_id, note_type, note_content, created_at, updated_at
                FROM task_notes
                WHERE task_id = ? AND is_deleted = 0
                ORDER BY id DESC
                LIMIT 1
                """,
                noteRowMapper,
                taskId
        );
        return rows.getFirst();
    }

    public TaskNote update(Long noteId, Long taskId, NoteType noteType, String noteContent, LocalDateTime now) {
        String sql = """
                UPDATE task_notes
                SET note_type = ?, note_content = ?, updated_at = ?
                WHERE id = ? AND task_id = ? AND is_deleted = 0
                """;
        int affectedRows = jdbcTemplate.update(
                sql,
                noteType.name(),
                normalizeText(noteContent),
                formatDateTime(now),
                noteId,
                taskId
        );
        if (affectedRows <= 0) {
            return null;
        }
        return findByIdAndTaskId(noteId, taskId).orElse(null);
    }

    public Optional<TaskNote> findByIdAndTaskId(Long noteId, Long taskId) {
        List<TaskNote> rows = jdbcTemplate.query(
                """
                SELECT id, task_id, note_type, note_content, created_at, updated_at
                FROM task_notes
                WHERE id = ? AND task_id = ? AND is_deleted = 0
                LIMIT 1
                """,
                noteRowMapper,
                noteId,
                taskId
        );
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.getFirst());
    }

    public boolean softDelete(Long noteId, Long taskId, LocalDateTime now) {
        int affectedRows = jdbcTemplate.update(
                """
                UPDATE task_notes
                SET is_deleted = 1, deleted_at = ?, updated_at = ?
                WHERE id = ? AND task_id = ? AND is_deleted = 0
                """,
                formatDateTime(now),
                formatDateTime(now),
                noteId,
                taskId
        );
        return affectedRows > 0;
    }

    public void replaceAll(Long taskId, List<TaskNoteResponse> notes, LocalDateTime now) {
        jdbcTemplate.update("UPDATE task_notes SET is_deleted = 1, deleted_at = ?, updated_at = ? WHERE task_id = ? AND is_deleted = 0", formatDateTime(now), formatDateTime(now), taskId);
        if (notes == null) return;
        for (TaskNoteResponse note : notes) {
            if (note.getNoteType() != null && note.getNoteContent() != null && !note.getNoteContent().isBlank()) {
                save(taskId, note.getNoteType(), note.getNoteContent(), now);
            }
        }
    }

    private String normalizeText(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
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
