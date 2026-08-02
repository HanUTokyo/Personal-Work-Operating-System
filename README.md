# Task App

Task App is a lightweight personal and small-team work operating system. The beta3 Web and Backend release combines project execution, personal priorities, knowledge notes, AI suggestions, and lightweight sharing.

Current release: `1.0.0-beta.3`

## Features

- Weekly and long-term personal task lists
- Project priorities, pinning, search, sorting, and stale-project indicators
- Hierarchical project phases with automatic progress calculation
- Project knowledge, rich-text notes, flash notes, and global AI suggestions
- Read-only and editable project sharing
- AI-friendly JSON export for one project or all projects
- English, Simplified Chinese, and Japanese UI
- Responsive light, dark, and system themes

## Technology

- Web: React 19, TypeScript, Vite, Mantine, and TipTap
- Backend: Java 21, Spring Boot 3.5, JDBC, and SQLite

## Repository Layout

```text
task-app/
├── frontend/       # React + TypeScript Web client
├── backend/        # Spring Boot REST API and database migrations
└── data/           # Local runtime database (ignored by Git)
```

## Local Development

Requirements:

- Java 21
- Node.js 22 or another Vite 7-compatible Node.js release
- `sqlite3`

Create a disposable English sample database from the committed schema and seed files:

```bash
./backend/scripts/init_sample_db.sh
```

> This command replaces your local `data/tasks.db`. Back up local data before running it.

Start the backend:

```bash
cd backend
./gradlew bootRun
```

Start the Web client in another terminal:

```bash
cd frontend
npm ci
npm run dev
```

Open `http://localhost:5173/` for English or `http://localhost:5173/zh.html` for Chinese. The local Web client connects to `http://localhost:8080/api` by default.

To use a different API endpoint:

```bash
cd frontend
cp .env.example .env.local
```

Then edit `VITE_TASK_API_BASE_URL` in `.env.local`.

The sample dataset includes a development-only account:

```text
Username: default
Password: default123
```

Never use the sample account or seed data in production.

## Backend Configuration

The backend supports these environment variables:

| Variable | Default | Purpose |
| --- | --- | --- |
| `TASKAPP_DB_URL` | `jdbc:sqlite:../data/tasks.db` | SQLite JDBC connection URL |
| `TASKAPP_CORS_ALLOWED_ORIGINS` | Local Vite/static-server origins | Comma-separated browser origins allowed to call `/api/**` |

Example:

```bash
export TASKAPP_DB_URL='jdbc:sqlite:/opt/task-app/data/tasks.db'
export TASKAPP_CORS_ALLOWED_ORIGINS='https://tasks.example.com'
cd backend
./gradlew bootRun
```

## Authentication Migration

- New passwords use bcrypt with cost 12.
- Existing legacy SHA-256 password hashes are accepted once and transparently upgraded after a successful login.
- The `beta3-invalidate-auth-tokens` database migration invalidates existing sessions exactly once, so users must sign in again after upgrading.

If a historical repository database contained real user records, notify those users to change any reused passwords. Removing the database from the current tree does not remove earlier Git objects.

## Verification

Run the release checks before opening or merging a pull request:

```bash
cd frontend && npm ci && npm run build
cd ../backend && ./gradlew test
```

## Data and Secrets

- Runtime SQLite databases, backups, logs, PID files, local environment files, signing files, and application binaries are ignored by Git.
- Commit `schema.sql`, migration code, and sample seed SQL instead of a generated `tasks.db`.
- Release signing credentials must remain outside the repository.

## Release Process

1. Work on a release branch and open a pull request into `main`.
2. Confirm CI passes and inspect the changed-file list for local data.
3. Merge only after review.
4. Create the annotated tag `v1.0.0-beta.3` on the merged commit.
5. Publish a GitHub prerelease from that tag.

Changes since beta1 are summarized in [CHANGELOG.md](CHANGELOG.md).

## Deployment Notes

Start the backend from the `backend` directory when using the default relative database path. Production deployments should provide HTTPS, reverse-proxy `/api` to Spring Boot, set an explicit CORS allowlist, maintain database backups, and use service supervision. Do not deploy the committed sample credentials.
