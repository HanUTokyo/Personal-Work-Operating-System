package com.taskapp.backend.repository;

import com.taskapp.backend.model.PhaseStatus;
import com.taskapp.backend.model.TaskPhase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class TaskPhaseRepository {

    private static final DateTimeFormatter DB_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<TaskPhase> phaseRowMapper = (rs, rowNum) -> {
        TaskPhase phase = new TaskPhase();
        phase.setId(rs.getLong("id"));
        phase.setTaskId(rs.getLong("task_id"));
        phase.setPhaseKey(rs.getString("phase_key"));
        phase.setParentPhaseKey(rs.getString("parent_phase_key"));
        phase.setPhaseName(rs.getString("phase_name"));
        phase.setPhaseDescription(rs.getString("phase_description"));
        phase.setPhaseStatus(PhaseStatus.valueOf(rs.getString("phase_status")));
        phase.setSortOrder(rs.getInt("sort_order"));
        phase.setCreatedAt(parseDateTime(rs.getString("created_at")));
        phase.setUpdatedAt(parseDateTime(rs.getString("updated_at")));
        return phase;
    };

    public TaskPhaseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TaskPhase> findByTaskId(Long taskId) {
        String sql = """
                SELECT id, task_id, phase_key, parent_phase_key, phase_name, phase_description, phase_status, sort_order, created_at, updated_at
                FROM task_phases
                WHERE task_id = ?
                ORDER BY sort_order ASC, id ASC
                """;
        return jdbcTemplate.query(sql, phaseRowMapper, taskId);
    }

    public Map<Long, List<TaskPhase>> findByTaskIds(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String placeholders = String.join(",", Collections.nCopies(taskIds.size(), "?"));
        String sql = """
                SELECT id, task_id, phase_key, parent_phase_key, phase_name, phase_description, phase_status, sort_order, created_at, updated_at
                FROM task_phases
                WHERE task_id IN (%s)
                ORDER BY task_id ASC, sort_order ASC, id ASC
                """.formatted(placeholders);

        List<TaskPhase> allPhases = jdbcTemplate.query(sql, phaseRowMapper, taskIds.toArray());
        Map<Long, List<TaskPhase>> phaseMap = new HashMap<>();

        for (TaskPhase phase : allPhases) {
            phaseMap.computeIfAbsent(phase.getTaskId(), ignored -> new ArrayList<>()).add(phase);
        }

        return phaseMap;
    }

    public void replaceAll(Long taskId, List<TaskPhase> phases, LocalDateTime now) {
        deleteByTaskId(taskId);
        insertAll(taskId, phases, now);
    }

    public void deleteByTaskId(Long taskId) {
        jdbcTemplate.update("DELETE FROM task_phases WHERE task_id = ?", taskId);
    }

    public void insertAll(Long taskId, List<TaskPhase> phases, LocalDateTime now) {
        String sql = """
                INSERT INTO task_phases (
                    task_id,
                    phase_key,
                    parent_phase_key,
                    phase_name,
                    phase_description,
                    phase_status,
                    sort_order,
                    created_at,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        for (int i = 0; i < phases.size(); i++) {
            TaskPhase phase = phases.get(i);
            int sortOrder = phase.getSortOrder() > 0 ? phase.getSortOrder() : i + 1;

            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql);
                ps.setLong(1, taskId);
                ps.setString(2, phase.getPhaseKey());
                ps.setString(3, phase.getParentPhaseKey());
                ps.setString(4, phase.getPhaseName());
                ps.setString(5, phase.getPhaseDescription());
                ps.setString(6, phase.getPhaseStatus().name());
                ps.setInt(7, sortOrder);
                ps.setString(8, formatDateTime(now));
                ps.setString(9, formatDateTime(now));
                return ps;
            });
        }
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
