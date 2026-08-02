INSERT OR IGNORE INTO tasks (
    id,
    task_title,
    task_description,
    phase1_status,
    phase2_status,
    phase3_status,
    priority,
    overall_progress,
    created_at,
    updated_at
) VALUES
    (
        1,
        'Oracle VM Environment Setup',
        'Prepare Linux VM, install Java 21, and validate runtime dependencies.',
        'DONE',
        'DOING',
        'TODO',
        'HIGH',
        50.0,
        '2026-03-07T09:00:00',
        '2026-03-08T14:20:00'
    ),
    (
        2,
        'Task API Integration',
        'Connect frontend fetch calls with backend REST API and align response fields.',
        'DONE',
        'DONE',
        'DOING',
        'HIGH',
        83.3,
        '2026-03-07T10:30:00',
        '2026-03-09T10:45:00'
    ),
    (
        3,
        'Regression Checklist',
        'Run CRUD, search, and sorting checks before release.',
        'TODO',
        'TODO',
        'TODO',
        'MEDIUM',
        0.0,
        '2026-03-09T08:00:00',
        '2026-03-09T08:00:00'
    ),
    (
        4,
        'Legacy Migration Workstream',
        'Legacy project kept for stale project highlight verification.',
        'DOING',
        'TODO',
        'TODO',
        'LOW',
        16.7,
        '2026-01-13T10:00:00',
        '2026-01-13T10:00:00'
    );

INSERT OR IGNORE INTO task_phases (
    task_id,
    phase_key,
    parent_phase_key,
    phase_name,
    phase_description,
    phase_status,
    sort_order,
    created_at,
    updated_at
) VALUES
    (1, 'phase-1', NULL, 'Infrastructure Baseline', 'Linux VM provisioning and base package installation completed.', 'DONE', 1, '2026-03-07T09:00:00', '2026-03-08T14:20:00'),
    (1, 'phase-2', 'phase-1', 'Runtime Configuration', 'Java 21 runtime and service startup scripts are in progress.', 'DOING', 2, '2026-03-07T09:00:00', '2026-03-08T14:20:00'),
    (1, 'phase-3', 'phase-1', 'Connectivity Validation', 'End-to-end connectivity and health checks pending.', 'TODO', 3, '2026-03-07T09:00:00', '2026-03-08T14:20:00'),
    (2, 'phase-1', NULL, 'API Baseline', 'Core REST endpoints implemented and validated.', 'DONE', 1, '2026-03-07T10:30:00', '2026-03-09T10:45:00'),
    (2, 'phase-2', 'phase-1', 'Frontend Mapping', 'Frontend payload and response mapping is complete.', 'DONE', 2, '2026-03-07T10:30:00', '2026-03-09T10:45:00'),
    (2, 'phase-3', 'phase-1', 'Edge Case Hardening', 'Validation and failure-path handling in progress.', 'DOING', 3, '2026-03-07T10:30:00', '2026-03-09T10:45:00'),
    (3, 'phase-1', NULL, 'Scope Review', 'Waiting for test window allocation.', 'TODO', 1, '2026-03-09T08:00:00', '2026-03-09T08:00:00'),
    (3, 'phase-2', 'phase-1', 'Execution Plan', 'Waiting for test window allocation.', 'TODO', 2, '2026-03-09T08:00:00', '2026-03-09T08:00:00'),
    (3, 'phase-3', 'phase-1', 'Sign-off', 'Waiting for test window allocation.', 'TODO', 3, '2026-03-09T08:00:00', '2026-03-09T08:00:00'),
    (4, 'phase-1', NULL, 'Legacy Audit', 'Project has been inactive and needs re-assessment.', 'DOING', 1, '2026-01-13T10:00:00', '2026-01-13T10:00:00'),
    (4, 'phase-2', 'phase-1', 'Refactor Plan', 'No active resource assignment yet.', 'TODO', 2, '2026-01-13T10:00:00', '2026-01-13T10:00:00'),
    (4, 'phase-3', 'phase-1', 'Execution', 'No active resource assignment yet.', 'TODO', 3, '2026-01-13T10:00:00', '2026-01-13T10:00:00');

INSERT OR IGNORE INTO task_knowledge (
    task_id,
    recent_decisions,
    recent_experiments,
    knowledge_highlights,
    created_at,
    updated_at
) VALUES
    (
        1,
        'Prioritize runtime stability before adding new feature scope.',
        'Validated Java 21 runtime and SQLite connectivity under local load.',
        'Dependency-first deployment order reduces integration risk significantly.',
        '2026-03-08T14:20:00',
        '2026-03-08T14:20:00'
    ),
    (
        2,
        'Standardized taskTitle/taskDescription naming across frontend and backend.',
        'Tested sorting and keyword filtering across multiple datasets.',
        'A unified API envelope simplifies frontend error handling and observability.',
        '2026-03-09T10:45:00',
        '2026-03-09T10:45:00'
    ),
    (
        3,
        'Stabilize regression checklist before visual enhancements.',
        'Ran baseline CRUD regression manually to verify workflow integrity.',
        'A minimal executable regression checklist prevents feature drift in rapid iteration.',
        '2026-03-09T08:00:00',
        '2026-03-09T08:00:00'
    ),
    (
        4,
        'Pause legacy stream and prioritize active delivery tracks.',
        'Reviewed old implementation approach without new code execution.',
        'Stale project indicators help prevent resource misallocation.',
        '2026-01-13T10:00:00',
        '2026-01-13T10:00:00'
    );

INSERT OR IGNORE INTO task_notes (
    id,
    task_id,
    note_type,
    note_content,
    created_at,
    updated_at
) VALUES
    (1, 1, 'RECENT_DECISIONS', 'Finalize VM baseline before adding non-critical features.', '2026-03-08T14:00:00', '2026-03-08T14:00:00'),
    (2, 1, 'RECENT_EXPERIMENTS', 'Verified stable startup profile with Java 21 and SQLite.', '2026-03-08T15:00:00', '2026-03-08T15:00:00'),
    (3, 2, 'KNOWLEDGE_HIGHLIGHTS', 'Unified API response envelope reduced frontend condition branching.', '2026-03-09T09:30:00', '2026-03-09T09:30:00'),
    (4, 2, 'RECENT_DECISIONS', 'Implement sorting and search consistency before visual polishing.', '2026-03-09T10:30:00', '2026-03-09T10:30:00'),
    (5, 3, 'RECENT_EXPERIMENTS', 'Validated empty-state and loading-state transitions with seeded data.', '2026-03-10T10:00:00', '2026-03-10T10:00:00');
