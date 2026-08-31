CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    password_salt TEXT NOT NULL,
    auth_token TEXT,
    onboarding_status TEXT NOT NULL DEFAULT 'ESTABLISHED',
    onboarding_guide_closed INTEGER NOT NULL DEFAULT 0,
    onboarding_ai_used INTEGER NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS app_migrations (
    migration_id TEXT PRIMARY KEY,
    applied_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS tasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_title TEXT NOT NULL,
    task_description TEXT,
    owner_user_id INTEGER,
    phase1_status TEXT NOT NULL,
    phase2_status TEXT NOT NULL,
    phase3_status TEXT NOT NULL,
    priority TEXT NOT NULL DEFAULT 'MEDIUM',
    overall_progress REAL NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    revision INTEGER NOT NULL DEFAULT 1,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    deleted_at DATETIME,
    is_archived INTEGER NOT NULL DEFAULT 0,
    archived_at DATETIME,
    is_demo INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_tasks_owner FOREIGN KEY(owner_user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS task_versions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    revision INTEGER NOT NULL,
    snapshot_json TEXT NOT NULL,
    change_reason TEXT NOT NULL,
    changed_by_user_id INTEGER NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_task_versions_task FOREIGN KEY(task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_versions_user FOREIGN KEY(changed_by_user_id) REFERENCES users(id)
);
CREATE INDEX IF NOT EXISTS idx_task_versions_task ON task_versions(task_id, id DESC);

CREATE TABLE IF NOT EXISTS task_conflict_drafts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    base_revision INTEGER NOT NULL,
    payload_json TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    resolved_at DATETIME,
    CONSTRAINT fk_task_conflict_drafts_task FOREIGN KEY(task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_conflict_drafts_user FOREIGN KEY(user_id) REFERENCES users(id)
);
CREATE INDEX IF NOT EXISTS idx_task_conflict_drafts_user_task ON task_conflict_drafts(user_id, task_id, id DESC);

CREATE TABLE IF NOT EXISTS task_phases (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    phase_key TEXT NOT NULL,
    parent_phase_key TEXT,
    phase_name TEXT NOT NULL,
    phase_description TEXT,
    phase_status TEXT NOT NULL,
    sort_order INTEGER NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_task_phases_task FOREIGN KEY(task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT uq_task_phase_key UNIQUE(task_id, phase_key),
    CONSTRAINT uq_task_phase_sort UNIQUE(task_id, sort_order)
);

CREATE TABLE IF NOT EXISTS task_knowledge (
    task_id INTEGER PRIMARY KEY,
    recent_decisions TEXT,
    recent_experiments TEXT,
    knowledge_highlights TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_task_knowledge_task FOREIGN KEY(task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS task_notes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    note_type TEXT NOT NULL,
    note_content TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    deleted_at DATETIME,
    CONSTRAINT fk_task_notes_task FOREIGN KEY(task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS flash_notes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_user_id INTEGER,
    note_content TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    deleted_at DATETIME,
    CONSTRAINT fk_flash_notes_owner FOREIGN KEY(owner_user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS personal_tasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_user_id INTEGER NOT NULL,
    task_type TEXT NOT NULL,
    content TEXT NOT NULL,
    completed INTEGER NOT NULL DEFAULT 0,
    pinned INTEGER NOT NULL DEFAULT 0,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    deleted_at DATETIME,
    CONSTRAINT fk_personal_tasks_owner FOREIGN KEY(owner_user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS global_ai_suggestions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_user_id INTEGER NOT NULL,
    content TEXT NOT NULL,
    suggestion_type TEXT NOT NULL DEFAULT 'AI',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    deleted_at DATETIME,
    CONSTRAINT fk_global_ai_suggestions_owner FOREIGN KEY(owner_user_id) REFERENCES users(id)
);

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
);

CREATE TABLE IF NOT EXISTS task_pins (
    task_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (task_id, user_id),
    CONSTRAINT fk_task_pins_task FOREIGN KEY(task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_pins_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_auth_token ON users(auth_token);
CREATE INDEX IF NOT EXISTS idx_tasks_title ON tasks(task_title);
CREATE INDEX IF NOT EXISTS idx_tasks_owner_user_id ON tasks(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_tasks_updated_at ON tasks(updated_at);
CREATE INDEX IF NOT EXISTS idx_tasks_progress ON tasks(overall_progress);
CREATE INDEX IF NOT EXISTS idx_tasks_is_archived ON tasks(is_archived);
CREATE INDEX IF NOT EXISTS idx_task_phases_task_id ON task_phases(task_id);
CREATE INDEX IF NOT EXISTS idx_task_phases_parent ON task_phases(task_id, parent_phase_key);
CREATE INDEX IF NOT EXISTS idx_task_knowledge_updated_at ON task_knowledge(updated_at);
CREATE INDEX IF NOT EXISTS idx_task_notes_task_id ON task_notes(task_id);
CREATE INDEX IF NOT EXISTS idx_task_notes_created_at ON task_notes(created_at);
CREATE INDEX IF NOT EXISTS idx_flash_notes_owner_user_id ON flash_notes(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_flash_notes_updated_at ON flash_notes(updated_at);
CREATE INDEX IF NOT EXISTS idx_personal_tasks_owner_type ON personal_tasks(owner_user_id, task_type);
CREATE INDEX IF NOT EXISTS idx_personal_tasks_active ON personal_tasks(owner_user_id, task_type, is_deleted, completed, created_at);
CREATE INDEX IF NOT EXISTS idx_personal_tasks_order ON personal_tasks(owner_user_id, task_type, is_deleted, pinned, sort_order);
CREATE INDEX IF NOT EXISTS idx_global_ai_suggestions_active ON global_ai_suggestions(owner_user_id, is_deleted, updated_at);
CREATE INDEX IF NOT EXISTS idx_task_shares_task_id ON task_shares(task_id);
CREATE INDEX IF NOT EXISTS idx_task_shares_user_id ON task_shares(shared_with_user_id);
CREATE INDEX IF NOT EXISTS idx_task_pins_user_id ON task_pins(user_id);
