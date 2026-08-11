package com.taskapp.backend.repository;

import com.taskapp.backend.model.AppUser;
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
import java.util.Map;
import java.util.Optional;

@Repository
public class UserRepository {

    private static final DateTimeFormatter DB_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<AppUser> userRowMapper = (rs, rowNum) -> {
        AppUser user = new AppUser();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setDisplayName(rs.getString("display_name"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setPasswordSalt(rs.getString("password_salt"));
        user.setAuthToken(rs.getString("auth_token"));
        user.setOnboardingStatus(rs.getString("onboarding_status"));
        user.setCreatedAt(parseDateTime(rs.getString("created_at")));
        user.setUpdatedAt(parseDateTime(rs.getString("updated_at")));
        return user;
    };

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<AppUser> findByUsername(String username) {
        List<AppUser> rows = jdbcTemplate.query(
                """
                SELECT id, username, display_name, password_hash, password_salt, auth_token, onboarding_status, created_at, updated_at
                FROM users
                WHERE LOWER(username) = LOWER(?)
                LIMIT 1
                """,
                userRowMapper,
                username
        );
        return rows.stream().findFirst();
    }

    public Optional<AppUser> findById(Long userId) {
        List<AppUser> rows = jdbcTemplate.query(
                """
                SELECT id, username, display_name, password_hash, password_salt, auth_token, onboarding_status, created_at, updated_at
                FROM users
                WHERE id = ?
                LIMIT 1
                """,
                userRowMapper,
                userId
        );
        return rows.stream().findFirst();
    }

    public Optional<AppUser> findByToken(String token) {
        List<AppUser> rows = jdbcTemplate.query(
                """
                SELECT id, username, display_name, password_hash, password_salt, auth_token, onboarding_status, created_at, updated_at
                FROM users
                WHERE auth_token = ?
                LIMIT 1
                """,
                userRowMapper,
                token
        );
        return rows.stream().findFirst();
    }

    public List<AppUser> findByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(userIds.size(), "?"));
        String sql = """
                SELECT id, username, display_name, password_hash, password_salt, auth_token, onboarding_status, created_at, updated_at
                FROM users
                WHERE id IN (%s)
                """.formatted(placeholders);
        return jdbcTemplate.query(sql, userRowMapper, userIds.toArray());
    }

    public Map<Long, AppUser> findMapByIds(List<Long> userIds) {
        return findByIds(userIds).stream().collect(java.util.stream.Collectors.toMap(AppUser::getId, user -> user));
    }

    public AppUser save(AppUser user) {
        String sql = """
                INSERT INTO users (username, display_name, password_hash, password_salt, auth_token, onboarding_status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getDisplayName());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getPasswordSalt());
            ps.setString(5, user.getAuthToken());
            ps.setString(6, user.getOnboardingStatus());
            ps.setString(7, formatDateTime(user.getCreatedAt()));
            ps.setString(8, formatDateTime(user.getUpdatedAt()));
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            user.setId(keyHolder.getKey().longValue());
        }
        return user;
    }

    public void updateToken(Long userId, String token, LocalDateTime now) {
        jdbcTemplate.update(
                """
                UPDATE users
                SET auth_token = ?, updated_at = ?
                WHERE id = ?
                """,
                token,
                formatDateTime(now),
                userId
        );
    }

    public void clearToken(Long userId, LocalDateTime now) {
        updateToken(userId, null, now);
    }

    public void updatePassword(Long userId, String passwordHash, String passwordSalt, LocalDateTime now) {
        jdbcTemplate.update(
                """
                UPDATE users
                SET password_hash = ?, password_salt = ?, updated_at = ?
                WHERE id = ?
                """,
                passwordHash,
                passwordSalt,
                formatDateTime(now),
                userId
        );
    }

    public void updateOnboardingStatus(Long userId, String status, LocalDateTime now) {
        jdbcTemplate.update("UPDATE users SET onboarding_status = ?, updated_at = ? WHERE id = ?", status, formatDateTime(now), userId);
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
