package com.taskapp.backend.config;

import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@DependsOn("sqliteMigrationConfig")
public class Beta3SecurityMigration {

    static final String INVALIDATE_AUTH_TOKENS = "beta3-invalidate-auth-tokens";

    public Beta3SecurityMigration(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS app_migrations (
                    migration_id TEXT PRIMARY KEY,
                    applied_at DATETIME NOT NULL
                )
                """);

        Integer applied = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_migrations WHERE migration_id = ?",
                Integer.class,
                INVALIDATE_AUTH_TOKENS
        );
        if (applied != null && applied > 0) {
            return;
        }

        jdbcTemplate.update("UPDATE users SET auth_token = NULL WHERE auth_token IS NOT NULL");
        jdbcTemplate.update(
                """
                INSERT INTO app_migrations (migration_id, applied_at)
                VALUES (?, strftime('%Y-%m-%dT%H:%M:%S','now'))
                """,
                INVALIDATE_AUTH_TOKENS
        );
    }
}
