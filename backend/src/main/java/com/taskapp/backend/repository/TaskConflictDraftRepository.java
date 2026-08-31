package com.taskapp.backend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class TaskConflictDraftRepository {
    private final JdbcTemplate jdbcTemplate;
    public TaskConflictDraftRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public Long save(Long taskId, Long userId, long baseRevision, String payloadJson, LocalDateTime now) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("INSERT INTO task_conflict_drafts (task_id, user_id, base_revision, payload_json, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, taskId); ps.setLong(2, userId); ps.setLong(3, baseRevision); ps.setString(4, payloadJson); ps.setString(5, now.toString()); ps.setString(6, now.toString());
            return ps;
        }, keys);
        return keys.getKey() == null ? null : keys.getKey().longValue();
    }

    public Optional<DraftRow> findLatestActive(Long taskId, Long userId) {
        List<DraftRow> rows = jdbcTemplate.query("SELECT id, payload_json, created_at FROM task_conflict_drafts WHERE task_id = ? AND user_id = ? AND resolved_at IS NULL ORDER BY id DESC LIMIT 1", (rs, row) -> new DraftRow(rs.getLong("id"), rs.getString("payload_json"), LocalDateTime.parse(rs.getString("created_at"))), taskId, userId);
        return rows.stream().findFirst();
    }

    public void resolve(Long id, Long userId, LocalDateTime now) {
        jdbcTemplate.update("UPDATE task_conflict_drafts SET resolved_at = ?, updated_at = ? WHERE id = ? AND user_id = ? AND resolved_at IS NULL", now.toString(), now.toString(), id, userId);
    }

    public record DraftRow(Long id, String payloadJson, LocalDateTime createdAt) { }
}
