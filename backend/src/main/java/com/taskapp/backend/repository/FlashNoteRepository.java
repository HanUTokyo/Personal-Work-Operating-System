package com.taskapp.backend.repository;

import com.taskapp.backend.model.FlashNote;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Repository
public class FlashNoteRepository {

    private static final DateTimeFormatter DB_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<FlashNote> flashNoteRowMapper = (rs, rowNum) -> {
        FlashNote note = new FlashNote();
        note.setId(rs.getLong("id"));
        note.setNoteContent(rs.getString("note_content"));
        note.setCreatedAt(parseDateTime(rs.getString("created_at")));
        note.setUpdatedAt(parseDateTime(rs.getString("updated_at")));
        return note;
    };

    public FlashNoteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<FlashNote> findAll(Long ownerUserId) {
        return jdbcTemplate.query(
                """
                SELECT id, note_content, created_at, updated_at
                FROM flash_notes
                WHERE is_deleted = 0 AND owner_user_id = ?
                ORDER BY updated_at DESC, id DESC
                """,
                flashNoteRowMapper,
                ownerUserId
        );
    }

    public FlashNote save(Long ownerUserId, String noteContent, LocalDateTime now) {
        jdbcTemplate.update(
                """
                INSERT INTO flash_notes (owner_user_id, note_content, created_at, updated_at)
                VALUES (?, ?, ?, ?)
                """,
                ownerUserId,
                normalizeText(noteContent),
                formatDateTime(now),
                formatDateTime(now)
        );

        List<FlashNote> rows = jdbcTemplate.query(
                """
                SELECT id, note_content, created_at, updated_at
                FROM flash_notes
                WHERE is_deleted = 0 AND owner_user_id = ?
                ORDER BY id DESC
                LIMIT 1
                """,
                flashNoteRowMapper,
                ownerUserId
        );
        return rows.getFirst();
    }

    public Optional<FlashNote> findById(Long ownerUserId, Long noteId) {
        List<FlashNote> rows = jdbcTemplate.query(
                """
                SELECT id, note_content, created_at, updated_at
                FROM flash_notes
                WHERE id = ? AND owner_user_id = ? AND is_deleted = 0
                LIMIT 1
                """,
                flashNoteRowMapper,
                noteId,
                ownerUserId
        );
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.getFirst());
    }

    public FlashNote update(Long ownerUserId, Long noteId, String noteContent, LocalDateTime now) {
        int affectedRows = jdbcTemplate.update(
                """
                UPDATE flash_notes
                SET note_content = ?, updated_at = ?
                WHERE id = ? AND owner_user_id = ? AND is_deleted = 0
                """,
                normalizeText(noteContent),
                formatDateTime(now),
                noteId,
                ownerUserId
        );
        if (affectedRows <= 0) {
            return null;
        }
        return findById(ownerUserId, noteId).orElse(null);
    }

    public boolean softDelete(Long ownerUserId, Long noteId, LocalDateTime now) {
        int affectedRows = jdbcTemplate.update(
                """
                UPDATE flash_notes
                SET is_deleted = 1, deleted_at = ?, updated_at = ?
                WHERE id = ? AND owner_user_id = ? AND is_deleted = 0
                """,
                formatDateTime(now),
                formatDateTime(now),
                noteId,
                ownerUserId
        );
        return affectedRows > 0;
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
