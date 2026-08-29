package com.taskapp.backend.repository;

import com.taskapp.backend.model.TaskVersion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class TaskVersionRepository {
    private final JdbcTemplate jdbcTemplate;
    public TaskVersionRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public void save(Long taskId, long revision, String snapshotJson, String reason, Long userId, LocalDateTime now) {
        jdbcTemplate.update("INSERT INTO task_versions (task_id, revision, snapshot_json, change_reason, changed_by_user_id, created_at) VALUES (?, ?, ?, ?, ?, ?)", taskId, revision, snapshotJson, reason, userId, now.toString());
    }

    public List<TaskVersion> findByTaskId(Long taskId) {
        return jdbcTemplate.query("""
                SELECT v.id, v.task_id, v.revision, v.snapshot_json, v.change_reason, v.changed_by_user_id, v.created_at, u.username
                FROM task_versions v JOIN users u ON u.id = v.changed_by_user_id
                WHERE v.task_id = ? ORDER BY v.id DESC LIMIT 50
                """, (rs, row) -> map(rs), taskId);
    }

    public Optional<TaskVersion> findByIdAndTaskId(Long id, Long taskId) {
        List<TaskVersion> rows = jdbcTemplate.query("""
                SELECT v.id, v.task_id, v.revision, v.snapshot_json, v.change_reason, v.changed_by_user_id, v.created_at, u.username
                FROM task_versions v JOIN users u ON u.id = v.changed_by_user_id
                WHERE v.id = ? AND v.task_id = ?
                """, (rs, row) -> map(rs), id, taskId);
        return rows.stream().findFirst();
    }

    private TaskVersion map(java.sql.ResultSet rs) throws java.sql.SQLException {
        TaskVersion value = new TaskVersion();
        value.setId(rs.getLong("id")); value.setTaskId(rs.getLong("task_id")); value.setRevision(rs.getLong("revision"));
        value.setSnapshotJson(rs.getString("snapshot_json")); value.setChangeReason(rs.getString("change_reason")); value.setChangedByUserId(rs.getLong("changed_by_user_id")); value.setChangedByUsername(rs.getString("username"));
        value.setCreatedAt(java.time.LocalDateTime.parse(rs.getString("created_at")));
        return value;
    }
}
