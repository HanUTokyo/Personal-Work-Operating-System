package com.taskapp.backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Beta3SecurityMigrationTest {

    @TempDir
    Path tempDir;

    @Test
    void invalidatesExistingTokensExactlyOnce() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:sqlite:" + tempDir.resolve("security-migration.db")
        );
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("CREATE TABLE users (id INTEGER PRIMARY KEY, auth_token TEXT)");
        jdbcTemplate.update("INSERT INTO users (id, auth_token) VALUES (1, 'old-token')");

        new Beta3SecurityMigration(jdbcTemplate);

        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE auth_token IS NOT NULL",
                Integer.class
        ));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_migrations WHERE migration_id = ?",
                Integer.class,
                Beta3SecurityMigration.INVALIDATE_AUTH_TOKENS
        ));

        jdbcTemplate.update("UPDATE users SET auth_token = 'new-token' WHERE id = 1");
        new Beta3SecurityMigration(jdbcTemplate);

        assertEquals("new-token", jdbcTemplate.queryForObject(
                "SELECT auth_token FROM users WHERE id = 1",
                String.class
        ));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_migrations WHERE migration_id = ?",
                Integer.class,
                Beta3SecurityMigration.INVALIDATE_AUTH_TOKENS
        ));
    }
}
