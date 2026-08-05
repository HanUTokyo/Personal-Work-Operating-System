package com.taskapp.backend.repository;

import com.taskapp.backend.model.GlobalAiSuggestion;
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
public class GlobalAiSuggestionRepository {

    private static final DateTimeFormatter DB_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<GlobalAiSuggestion> rowMapper = (rs, rowNum) -> {
        GlobalAiSuggestion suggestion = new GlobalAiSuggestion();
        suggestion.setId(rs.getLong("id"));
        suggestion.setContent(rs.getString("content"));
        suggestion.setCreatedAt(parseDateTime(rs.getString("created_at")));
        suggestion.setUpdatedAt(parseDateTime(rs.getString("updated_at")));
        return suggestion;
    };

    public GlobalAiSuggestionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<GlobalAiSuggestion> findAll(Long ownerUserId) { return findAll(ownerUserId, "AI"); }

    public List<GlobalAiSuggestion> findAll(Long ownerUserId, String type) {
        return jdbcTemplate.query("""
                SELECT id, content, created_at, updated_at
                FROM global_ai_suggestions
                WHERE owner_user_id = ? AND suggestion_type = ? AND is_deleted = 0
                ORDER BY updated_at DESC, id DESC
                """, rowMapper, ownerUserId, type);
    }

    public GlobalAiSuggestion save(Long ownerUserId, String type, String content, LocalDateTime now) {
        jdbcTemplate.update("""
                INSERT INTO global_ai_suggestions (owner_user_id, suggestion_type, content, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?)
                """, ownerUserId, type, normalizeContent(content), formatDateTime(now), formatDateTime(now));
        return jdbcTemplate.query("""
                SELECT id, content, created_at, updated_at
                FROM global_ai_suggestions
                WHERE owner_user_id = ? AND suggestion_type = ? AND is_deleted = 0
                ORDER BY id DESC
                LIMIT 1
                """, rowMapper, ownerUserId, type).getFirst();
    }

    public GlobalAiSuggestion save(Long ownerUserId, String content, LocalDateTime now) { return save(ownerUserId, "AI", content, now); }

    public Optional<GlobalAiSuggestion> findById(Long ownerUserId, Long suggestionId, String type) {
        List<GlobalAiSuggestion> rows = jdbcTemplate.query("""
                SELECT id, content, created_at, updated_at
                FROM global_ai_suggestions
                WHERE id = ? AND owner_user_id = ? AND suggestion_type = ? AND is_deleted = 0
                LIMIT 1
                """, rowMapper, suggestionId, ownerUserId, type);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
    }

    public Optional<GlobalAiSuggestion> findById(Long ownerUserId, Long suggestionId) { return findById(ownerUserId, suggestionId, "AI"); }

    public GlobalAiSuggestion update(Long ownerUserId, Long suggestionId, String type, String content, LocalDateTime now) {
        int changed = jdbcTemplate.update("""
                UPDATE global_ai_suggestions
                SET content = ?, updated_at = ?
                WHERE id = ? AND owner_user_id = ? AND suggestion_type = ? AND is_deleted = 0
                """, normalizeContent(content), formatDateTime(now), suggestionId, ownerUserId, type);
        return changed > 0 ? findById(ownerUserId, suggestionId, type).orElse(null) : null;
    }

    public GlobalAiSuggestion update(Long ownerUserId, Long suggestionId, String content, LocalDateTime now) { return update(ownerUserId, suggestionId, "AI", content, now); }

    public boolean softDelete(Long ownerUserId, Long suggestionId, String type, LocalDateTime now) {
        return jdbcTemplate.update("""
                UPDATE global_ai_suggestions
                SET is_deleted = 1, deleted_at = ?, updated_at = ?
                WHERE id = ? AND owner_user_id = ? AND suggestion_type = ? AND is_deleted = 0
                """, formatDateTime(now), formatDateTime(now), suggestionId, ownerUserId, type) > 0;
    }

    public boolean softDelete(Long ownerUserId, Long suggestionId, LocalDateTime now) { return softDelete(ownerUserId, suggestionId, "AI", now); }

    private String normalizeContent(String content) {
        return content == null ? null : content.trim();
    }

    private String formatDateTime(LocalDateTime value) {
        return value.truncatedTo(ChronoUnit.SECONDS).format(DB_DATE_TIME_FORMATTER);
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value, DB_DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            return LocalDateTime.parse(value);
        }
    }
}
