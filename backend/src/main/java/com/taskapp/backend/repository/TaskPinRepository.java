package com.taskapp.backend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Repository
public class TaskPinRepository {

    private static final DateTimeFormatter DB_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    public TaskPinRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setPinned(Long taskId, Long userId, boolean pinned, LocalDateTime now) {
        if (!pinned) {
            jdbcTemplate.update("DELETE FROM task_pins WHERE task_id = ? AND user_id = ?", taskId, userId);
            return;
        }
        String timestamp = formatDateTime(now);
        jdbcTemplate.update("""
                INSERT INTO task_pins (task_id, user_id, created_at, updated_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(task_id, user_id) DO UPDATE SET updated_at = excluded.updated_at
                """, taskId, userId, timestamp, timestamp);
    }

    private String formatDateTime(LocalDateTime value) {
        return value.truncatedTo(ChronoUnit.SECONDS).format(DB_DATE_TIME_FORMATTER);
    }
}
