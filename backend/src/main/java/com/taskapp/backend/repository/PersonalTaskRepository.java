package com.taskapp.backend.repository;

import com.taskapp.backend.model.PersonalTask;
import com.taskapp.backend.model.PersonalTaskType;
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
public class PersonalTaskRepository {

    private static final DateTimeFormatter DB_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<PersonalTask> rowMapper = (rs, rowNum) -> {
        PersonalTask task = new PersonalTask();
        task.setId(rs.getLong("id"));
        task.setTaskType(PersonalTaskType.valueOf(rs.getString("task_type")));
        task.setContent(rs.getString("content"));
        task.setCompleted(rs.getInt("completed") != 0);
        task.setPinned(rs.getInt("pinned") != 0);
        task.setSortOrder(rs.getInt("sort_order"));
        task.setCreatedAt(parseDateTime(rs.getString("created_at")));
        task.setUpdatedAt(parseDateTime(rs.getString("updated_at")));
        return task;
    };

    public PersonalTaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PersonalTask> findAll(Long ownerUserId, PersonalTaskType type) {
        return jdbcTemplate.query("""
                SELECT id, task_type, content, completed, pinned, sort_order, created_at, updated_at
                FROM personal_tasks
                WHERE owner_user_id = ? AND task_type = ? AND is_deleted = 0
                ORDER BY pinned DESC, sort_order ASC, id ASC
                """, rowMapper, ownerUserId, type.name());
    }

    public PersonalTask save(Long ownerUserId, PersonalTaskType type, String content, LocalDateTime now) {
        Integer nextSortOrder = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(sort_order), 0) + 1
                FROM personal_tasks
                WHERE owner_user_id = ? AND task_type = ? AND is_deleted = 0
                """, Integer.class, ownerUserId, type.name());
        jdbcTemplate.update("""
                INSERT INTO personal_tasks (owner_user_id, task_type, content, completed, pinned, sort_order, created_at, updated_at)
                VALUES (?, ?, ?, 0, 0, ?, ?, ?)
                """, ownerUserId, type.name(), normalizeContent(content), nextSortOrder, formatDateTime(now), formatDateTime(now));
        return jdbcTemplate.query("""
                SELECT id, task_type, content, completed, pinned, sort_order, created_at, updated_at
                FROM personal_tasks
                WHERE owner_user_id = ? AND is_deleted = 0
                ORDER BY id DESC
                LIMIT 1
                """, rowMapper, ownerUserId).getFirst();
    }

    public Optional<PersonalTask> findById(Long ownerUserId, Long taskId) {
        List<PersonalTask> tasks = jdbcTemplate.query("""
                SELECT id, task_type, content, completed, pinned, sort_order, created_at, updated_at
                FROM personal_tasks
                WHERE id = ? AND owner_user_id = ? AND is_deleted = 0
                LIMIT 1
                """, rowMapper, taskId, ownerUserId);
        return tasks.isEmpty() ? Optional.empty() : Optional.of(tasks.getFirst());
    }

    public PersonalTask update(Long ownerUserId, Long taskId, String content, boolean completed, boolean pinned, LocalDateTime now) {
        int changed = jdbcTemplate.update("""
                UPDATE personal_tasks
                SET content = ?, completed = ?, pinned = ?, updated_at = ?
                WHERE id = ? AND owner_user_id = ? AND is_deleted = 0
                """, normalizeContent(content), completed ? 1 : 0, pinned ? 1 : 0, formatDateTime(now), taskId, ownerUserId);
        return changed > 0 ? findById(ownerUserId, taskId).orElse(null) : null;
    }

    public void updateSortOrder(Long ownerUserId, Long taskId, int sortOrder, LocalDateTime now) {
        jdbcTemplate.update("""
                UPDATE personal_tasks
                SET sort_order = ?, updated_at = ?
                WHERE id = ? AND owner_user_id = ? AND is_deleted = 0
                """, sortOrder, formatDateTime(now), taskId, ownerUserId);
    }

    public boolean softDelete(Long ownerUserId, Long taskId, LocalDateTime now) {
        return jdbcTemplate.update("""
                UPDATE personal_tasks
                SET is_deleted = 1, deleted_at = ?, updated_at = ?
                WHERE id = ? AND owner_user_id = ? AND is_deleted = 0
                """, formatDateTime(now), formatDateTime(now), taskId, ownerUserId) > 0;
    }

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
