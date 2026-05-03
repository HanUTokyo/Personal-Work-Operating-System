package com.taskapp.backend.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SqliteMigrationConfig {

    private final JdbcTemplate jdbcTemplate;

    public SqliteMigrationConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        ensureUsersTable();
        ensureDefaultUser();
        ensurePriorityColumn();
        ensureTaskOwnerColumn();
        ensureSoftDeleteColumns();
        ensurePhaseDescriptionColumn();
        ensureKnowledgeTable();
        ensureTaskNotesTable();
        ensureFlashNotesTable();
        ensureFlashNoteOwnerColumn();
        ensureTaskSharesTable();
        ensureTaskNotesSoftDeleteColumns();
        ensureFlashNotesSoftDeleteColumns();
    }

    private void ensureUsersTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    display_name TEXT NOT NULL,
                    password_hash TEXT NOT NULL,
                    password_salt TEXT NOT NULL,
                    auth_token TEXT,
                    created_at DATETIME NOT NULL,
                    updated_at DATETIME NOT NULL
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_users_username ON users(username)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_users_auth_token ON users(auth_token)");
    }

    private void ensureDefaultUser() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE LOWER(username) = LOWER('default')",
                Integer.class
        );
        if (count == null || count == 0) {
            jdbcTemplate.update(
                    """
                    INSERT INTO users (username, display_name, password_hash, password_salt, created_at, updated_at)
                    VALUES ('default', 'Default User', ?, 'default-salt-v1', strftime('%Y-%m-%dT%H:%M:%S','now'), strftime('%Y-%m-%dT%H:%M:%S','now'))
                    """,
                    "27ec8991eab073477465d8b4bd290bea3dbc12dccc2b2e820aad3921668c5edd"
            );
        }
    }

    private void ensurePriorityColumn() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("PRAGMA table_info(tasks)");
        boolean hasPriority = columns.stream()
                .map(column -> String.valueOf(column.get("name")))
                .anyMatch(name -> "priority".equalsIgnoreCase(name));

        if (!hasPriority) {
            jdbcTemplate.execute("ALTER TABLE tasks ADD COLUMN priority TEXT NOT NULL DEFAULT 'MEDIUM'");
        }

        jdbcTemplate.update("UPDATE tasks SET priority = 'MEDIUM' WHERE priority IS NULL OR TRIM(priority) = ''");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_tasks_priority ON tasks(priority)");
    }

    private void ensureKnowledgeTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS task_knowledge (
                    task_id INTEGER PRIMARY KEY,
                    recent_decisions TEXT,
                    recent_experiments TEXT,
                    knowledge_highlights TEXT,
                    created_at DATETIME NOT NULL,
                    updated_at DATETIME NOT NULL,
                    CONSTRAINT fk_task_knowledge_task FOREIGN KEY(task_id) REFERENCES tasks(id) ON DELETE CASCADE
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_task_knowledge_updated_at ON task_knowledge(updated_at)");
    }

    private void ensureSoftDeleteColumns() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("PRAGMA table_info(tasks)");

        boolean hasIsDeleted = columns.stream()
                .map(column -> String.valueOf(column.get("name")))
                .anyMatch(name -> "is_deleted".equalsIgnoreCase(name));
        if (!hasIsDeleted) {
            jdbcTemplate.execute("ALTER TABLE tasks ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0");
        }

        boolean hasDeletedAt = columns.stream()
                .map(column -> String.valueOf(column.get("name")))
                .anyMatch(name -> "deleted_at".equalsIgnoreCase(name));
        if (!hasDeletedAt) {
            jdbcTemplate.execute("ALTER TABLE tasks ADD COLUMN deleted_at DATETIME");
        }

        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_tasks_is_deleted ON tasks(is_deleted)");
    }

    private void ensureTaskOwnerColumn() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("PRAGMA table_info(tasks)");
        boolean hasOwnerUserId = columns.stream()
                .map(column -> String.valueOf(column.get("name")))
                .anyMatch(name -> "owner_user_id".equalsIgnoreCase(name));
        if (!hasOwnerUserId) {
            jdbcTemplate.execute("ALTER TABLE tasks ADD COLUMN owner_user_id INTEGER");
        }

        Long defaultUserId = findDefaultUserId();
        jdbcTemplate.update("UPDATE tasks SET owner_user_id = ? WHERE owner_user_id IS NULL", defaultUserId);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_tasks_owner_user_id ON tasks(owner_user_id)");
    }

    private void ensurePhaseDescriptionColumn() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("PRAGMA table_info(task_phases)");
        boolean hasPhaseDescription = columns.stream()
                .map(column -> String.valueOf(column.get("name")))
                .anyMatch(name -> "phase_description".equalsIgnoreCase(name));

        if (!hasPhaseDescription) {
            jdbcTemplate.execute("ALTER TABLE task_phases ADD COLUMN phase_description TEXT");
        }
    }

    private void ensureTaskNotesTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS task_notes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    task_id INTEGER NOT NULL,
                    note_type TEXT NOT NULL,
                    note_content TEXT NOT NULL,
                    created_at DATETIME NOT NULL,
                    updated_at DATETIME NOT NULL,
                    CONSTRAINT fk_task_notes_task FOREIGN KEY(task_id) REFERENCES tasks(id) ON DELETE CASCADE
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_task_notes_task_id ON task_notes(task_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_task_notes_created_at ON task_notes(created_at)");
    }

    private void ensureFlashNotesTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS flash_notes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    owner_user_id INTEGER,
                    note_content TEXT NOT NULL,
                    created_at DATETIME NOT NULL,
                    updated_at DATETIME NOT NULL,
                    CONSTRAINT fk_flash_notes_owner FOREIGN KEY(owner_user_id) REFERENCES users(id)
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_flash_notes_updated_at ON flash_notes(updated_at)");
    }

    private void ensureFlashNoteOwnerColumn() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("PRAGMA table_info(flash_notes)");
        boolean hasOwnerUserId = columns.stream()
                .map(column -> String.valueOf(column.get("name")))
                .anyMatch(name -> "owner_user_id".equalsIgnoreCase(name));
        if (!hasOwnerUserId) {
            jdbcTemplate.execute("ALTER TABLE flash_notes ADD COLUMN owner_user_id INTEGER");
        }

        Long defaultUserId = findDefaultUserId();
        jdbcTemplate.update("UPDATE flash_notes SET owner_user_id = ? WHERE owner_user_id IS NULL", defaultUserId);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_flash_notes_owner_user_id ON flash_notes(owner_user_id)");
    }

    private void ensureTaskSharesTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS task_shares (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    task_id INTEGER NOT NULL,
                    shared_with_user_id INTEGER NOT NULL,
                    permission TEXT NOT NULL,
                    created_at DATETIME NOT NULL,
                    updated_at DATETIME NOT NULL,
                    CONSTRAINT fk_task_shares_task FOREIGN KEY(task_id) REFERENCES tasks(id) ON DELETE CASCADE,
                    CONSTRAINT fk_task_shares_user FOREIGN KEY(shared_with_user_id) REFERENCES users(id) ON DELETE CASCADE,
                    CONSTRAINT uq_task_share_user UNIQUE(task_id, shared_with_user_id)
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_task_shares_task_id ON task_shares(task_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_task_shares_user_id ON task_shares(shared_with_user_id)");
    }

    private void ensureTaskNotesSoftDeleteColumns() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("PRAGMA table_info(task_notes)");
        boolean hasIsDeleted = columns.stream()
                .map(column -> String.valueOf(column.get("name")))
                .anyMatch(name -> "is_deleted".equalsIgnoreCase(name));
        if (!hasIsDeleted) {
            jdbcTemplate.execute("ALTER TABLE task_notes ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0");
        }

        boolean hasDeletedAt = columns.stream()
                .map(column -> String.valueOf(column.get("name")))
                .anyMatch(name -> "deleted_at".equalsIgnoreCase(name));
        if (!hasDeletedAt) {
            jdbcTemplate.execute("ALTER TABLE task_notes ADD COLUMN deleted_at DATETIME");
        }

        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_task_notes_is_deleted ON task_notes(is_deleted)");
    }

    private void ensureFlashNotesSoftDeleteColumns() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("PRAGMA table_info(flash_notes)");
        boolean hasIsDeleted = columns.stream()
                .map(column -> String.valueOf(column.get("name")))
                .anyMatch(name -> "is_deleted".equalsIgnoreCase(name));
        if (!hasIsDeleted) {
            jdbcTemplate.execute("ALTER TABLE flash_notes ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0");
        }

        boolean hasDeletedAt = columns.stream()
                .map(column -> String.valueOf(column.get("name")))
                .anyMatch(name -> "deleted_at".equalsIgnoreCase(name));
        if (!hasDeletedAt) {
            jdbcTemplate.execute("ALTER TABLE flash_notes ADD COLUMN deleted_at DATETIME");
        }

        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_flash_notes_is_deleted ON flash_notes(is_deleted)");
    }

    private Long findDefaultUserId() {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE LOWER(username) = LOWER('default') LIMIT 1",
                Long.class
        );
    }
}
