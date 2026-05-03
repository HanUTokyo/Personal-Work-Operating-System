package com.taskapp.backend.repository;

import com.taskapp.backend.model.SharePermission;
import com.taskapp.backend.model.TaskShare;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Repository
public class TaskShareRepository {

    private static final DateTimeFormatter DB_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<TaskShare> shareRowMapper = (rs, rowNum) -> {
        TaskShare share = new TaskShare();
        share.setId(rs.getLong("id"));
        share.setTaskId(rs.getLong("task_id"));
        share.setSharedWithUserId(rs.getLong("shared_with_user_id"));
        share.setPermission(SharePermission.valueOf(rs.getString("permission")));
        share.setCreatedAt(parseDateTime(rs.getString("created_at")));
        share.setUpdatedAt(parseDateTime(rs.getString("updated_at")));
        return share;
    };

    public TaskShareRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TaskShare> findByTaskId(Long taskId) {
        return jdbcTemplate.query(
                """
                SELECT id, task_id, shared_with_user_id, permission, created_at, updated_at
                FROM task_shares
                WHERE task_id = ?
                ORDER BY id ASC
                """,
                shareRowMapper,
                taskId
        );
    }

    public Optional<TaskShare> findByTaskIdAndUserId(Long taskId, Long userId) {
        List<TaskShare> rows = jdbcTemplate.query(
                """
                SELECT id, task_id, shared_with_user_id, permission, created_at, updated_at
                FROM task_shares
                WHERE task_id = ? AND shared_with_user_id = ?
                LIMIT 1
                """,
                shareRowMapper,
                taskId,
                userId
        );
        return rows.stream().findFirst();
    }

    public TaskShare upsert(Long taskId, Long sharedWithUserId, SharePermission permission, LocalDateTime now) {
        Optional<TaskShare> existing = findByTaskIdAndUserId(taskId, sharedWithUserId);
        if (existing.isPresent()) {
            jdbcTemplate.update(
                    """
                    UPDATE task_shares
                    SET permission = ?, updated_at = ?
                    WHERE id = ?
                    """,
                    permission.name(),
                    formatDateTime(now),
                    existing.get().getId()
            );
            return findByTaskIdAndUserId(taskId, sharedWithUserId).orElseThrow();
        }

        String sql = """
                INSERT INTO task_shares (task_id, shared_with_user_id, permission, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, taskId);
            ps.setLong(2, sharedWithUserId);
            ps.setString(3, permission.name());
            ps.setString(4, formatDateTime(now));
            ps.setString(5, formatDateTime(now));
            return ps;
        }, keyHolder);
        Long id = keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
        return findById(id).orElseThrow();
    }

    public Optional<TaskShare> findById(Long shareId) {
        List<TaskShare> rows = jdbcTemplate.query(
                """
                SELECT id, task_id, shared_with_user_id, permission, created_at, updated_at
                FROM task_shares
                WHERE id = ?
                LIMIT 1
                """,
                shareRowMapper,
                shareId
        );
        return rows.stream().findFirst();
    }

    public boolean delete(Long taskId, Long shareId) {
        int affectedRows = jdbcTemplate.update(
                "DELETE FROM task_shares WHERE task_id = ? AND id = ?",
                taskId,
                shareId
        );
        return affectedRows > 0;
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
