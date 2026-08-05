package com.taskapp.backend.repository;

import com.taskapp.backend.model.PhaseStatus;
import com.taskapp.backend.model.ProjectPriority;
import com.taskapp.backend.model.Task;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class TaskRepository {

    private static final DateTimeFormatter DB_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Task> taskRowMapper = (rs, rowNum) -> {
        Task task = new Task();
        task.setId(rs.getLong("id"));
        task.setTaskTitle(rs.getString("task_title"));
        task.setTaskDescription(rs.getString("task_description"));
        task.setOwnerUserId(rs.getLong("owner_user_id"));
        task.setPhase1Status(PhaseStatus.valueOf(rs.getString("phase1_status")));
        task.setPhase2Status(PhaseStatus.valueOf(rs.getString("phase2_status")));
        task.setPhase3Status(PhaseStatus.valueOf(rs.getString("phase3_status")));
        task.setPriority(parsePriority(rs.getString("priority")));
        task.setOverallProgress(rs.getDouble("overall_progress"));
        task.setPinned(rs.getInt("pinned") != 0);
        task.setArchived(rs.getInt("is_archived") != 0);
        task.setArchivedAt(parseDateTime(rs.getString("archived_at")));
        task.setCreatedAt(parseDateTime(rs.getString("created_at")));
        task.setUpdatedAt(parseDateTime(rs.getString("updated_at")));
        return task;
    };

    public TaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Task> findAllForUser(Long userId, String keyword, String sortBy, String order) {
        return findAllForUser(userId, keyword, sortBy, order, false);
    }

    public List<Task> findAllForUser(Long userId, String keyword, String sortBy, String order, boolean archived) {
        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT tasks.id, tasks.task_title, tasks.task_description, tasks.owner_user_id,
                       tasks.phase1_status, tasks.phase2_status, tasks.phase3_status,
                       tasks.priority, tasks.overall_progress, tasks.created_at, tasks.updated_at, tasks.is_archived, tasks.archived_at,
                       CASE WHEN task_pins.task_id IS NULL THEN 0 ELSE 1 END AS pinned
                FROM tasks
                LEFT JOIN task_shares ON task_shares.task_id = tasks.id
                LEFT JOIN task_pins ON task_pins.task_id = tasks.id AND task_pins.user_id = ?
                WHERE tasks.is_deleted = 0
                  AND tasks.is_archived = ?
                  AND (tasks.owner_user_id = ? OR task_shares.shared_with_user_id = ?)
                """);

        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.add(archived ? 1 : 0);
        params.add(userId);
        params.add(userId);
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND LOWER(tasks.task_title) LIKE ?");
            params.add("%" + keyword.trim().toLowerCase(Locale.ROOT) + "%");
        }

        sql.append(" ORDER BY CASE WHEN task_pins.task_id IS NULL THEN 0 ELSE 1 END DESC, ")
                .append(resolveSortColumn(sortBy)).append(" ").append(resolveSortDirection(order));
        return jdbcTemplate.query(sql.toString(), taskRowMapper, params.toArray());
    }

    public Optional<Task> findById(Long id) {
        String sql = """
                SELECT id, task_title, task_description, owner_user_id, phase1_status, phase2_status, phase3_status,
                       priority, overall_progress, created_at, updated_at, 0 AS pinned, is_archived, archived_at
                FROM tasks
                WHERE id = ?
                  AND is_deleted = 0
                """;
        List<Task> tasks = jdbcTemplate.query(sql, taskRowMapper, id);
        return tasks.stream().findFirst();
    }

    public Task save(Task task) {
        String sql = """
                INSERT INTO tasks (
                    task_title, task_description, owner_user_id, phase1_status, phase2_status, phase3_status,
                    priority, overall_progress, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, task.getTaskTitle());
            ps.setString(2, task.getTaskDescription());
            ps.setLong(3, task.getOwnerUserId());
            ps.setString(4, task.getPhase1Status().name());
            ps.setString(5, task.getPhase2Status().name());
            ps.setString(6, task.getPhase3Status().name());
            ps.setString(7, task.getPriority().name());
            ps.setDouble(8, task.getOverallProgress());
            ps.setString(9, formatDateTime(task.getCreatedAt()));
            ps.setString(10, formatDateTime(task.getUpdatedAt()));
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            task.setId(keyHolder.getKey().longValue());
        }
        return task;
    }

    public Task update(Task task) {
        String sql = """
                UPDATE tasks
                SET task_title = ?,
                    task_description = ?,
                    phase1_status = ?,
                    phase2_status = ?,
                    phase3_status = ?,
                    priority = ?,
                    overall_progress = ?,
                    updated_at = ?
                WHERE id = ?
                """;

        jdbcTemplate.update(
                sql,
                task.getTaskTitle(),
                task.getTaskDescription(),
                task.getPhase1Status().name(),
                task.getPhase2Status().name(),
                task.getPhase3Status().name(),
                task.getPriority().name(),
                task.getOverallProgress(),
                formatDateTime(task.getUpdatedAt()),
                task.getId()
        );

        return task;
    }

    public boolean deleteById(Long id) {
        int affectedRows = jdbcTemplate.update(
                """
                UPDATE tasks
                SET is_deleted = 1,
                    deleted_at = ?,
                    updated_at = ?
                WHERE id = ?
                  AND is_deleted = 0
                """,
                formatDateTime(LocalDateTime.now()),
                formatDateTime(LocalDateTime.now()),
                id
        );
        return affectedRows > 0;
    }

    public void setArchived(Long id, boolean archived, LocalDateTime now) {
        jdbcTemplate.update("UPDATE tasks SET is_archived = ?, archived_at = ?, updated_at = ? WHERE id = ? AND is_deleted = 0",
                archived ? 1 : 0, archived ? formatDateTime(now) : null, formatDateTime(now), id);
    }

    private String resolveSortColumn(String sortBy) {
        if ("overallProgress".equalsIgnoreCase(sortBy)) {
            return "tasks.overall_progress";
        }
        if ("priority".equalsIgnoreCase(sortBy)) {
            return "CASE tasks.priority WHEN 'HIGH' THEN 3 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 1 ELSE 0 END";
        }
        if ("createdAt".equalsIgnoreCase(sortBy)) {
            return "tasks.created_at";
        }
        if ("taskTitle".equalsIgnoreCase(sortBy)) {
            return "tasks.task_title";
        }
        return "CASE tasks.priority WHEN 'HIGH' THEN 3 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 1 ELSE 0 END";
    }

    private String resolveSortDirection(String order) {
        return "asc".equalsIgnoreCase(order) ? "ASC" : "DESC";
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

    private ProjectPriority parsePriority(String value) {
        if (value == null || value.isBlank()) {
            return ProjectPriority.MEDIUM;
        }

        try {
            return ProjectPriority.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return ProjectPriority.MEDIUM;
        }
    }
}
