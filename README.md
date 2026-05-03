# Task Progress Management System

A lightweight project progress management system built with:

- Frontend: HTML + CSS + Vanilla JavaScript
- Backend: Java 21 + Spring Boot 3.x
- Database: SQLite

The app supports per-user task data and lightweight task sharing for family or small group use.

## 1. Project Structure

```text
/Users/kaihan/task-app/
├─ frontend/
│  ├─ index.html          # English default UI
│  ├─ zh.html             # Chinese UI
│  ├─ app.en.js
│  ├─ app.zh.js
│  └─ style.css
├─ backend/
│  ├─ src/main/java/...
│  ├─ src/main/resources/
│  │  ├─ application.yml
│  │  ├─ schema.sql
│  │  ├─ data.sql
│  │  └─ sample-data-en.sql
│  ├─ scripts/
│  │  └─ init_sample_db.sh
│  └─ build.gradle
└─ data/
   └─ tasks.db
```

## 2. Database Construction (Sample Method)

To build a fresh SQLite database with pure English sample data:

```bash
cd /Users/kaihan/task-app
./backend/scripts/init_sample_db.sh
```

What it does:

1. Removes old `/Users/kaihan/task-app/data/tasks.db`
2. Applies `/Users/kaihan/task-app/backend/src/main/resources/schema.sql`
3. Seeds `/Users/kaihan/task-app/backend/src/main/resources/sample-data-en.sql`

## 3. Run Locally

### Default Account For Existing Sample Data

Runtime migration creates a default owner for existing tasks:

```text
Username: default
Password: default123
```

Use this account to log in after starting the app. New users can also register from the login screen.

### Start Backend

For normal local development against the existing `data/tasks.db`, disable SQL seed initialization so the sample insert scripts do not touch SQLite sequences on every boot:

```bash
cd /Users/kaihan/task-app/backend
./gradlew bootRun --args='--spring.sql.init.mode=never'
```

Backend API base URL:

```text
http://localhost:8080/api
```

Use the plain `./gradlew bootRun` only when you intentionally want Spring SQL init to run. For a clean sample reset, prefer:

```bash
cd /Users/kaihan/task-app
./backend/scripts/init_sample_db.sh
cd backend
./gradlew bootRun
```

### Start Frontend (Static)

In a second terminal:

```bash
cd /Users/kaihan/task-app/frontend
python3 -m http.server 5500
```

Open in browser:

- English UI: `http://localhost:5500/index.html`
- Chinese UI: `http://localhost:5500/zh.html`

### Quick Smoke Test

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"default","password":"default123"}' \
  | sed -E 's/.*"token":"([^"]+)".*/\1/')

curl -s -H "Authorization: Bearer $TOKEN" \
  'http://localhost:8080/api/tasks?sortBy=priority&order=desc'
```

The response should contain `"success":true` and a task list.

### Stop Local Servers

Press `Ctrl+C` in both the backend and frontend terminal windows.

## 4. Key Features

- Multi-project progress tracking
- User login and per-user task isolation
- Lightweight task sharing with `VIEW` and `EDIT` permissions
- Dynamic phases per project
- Phase description support
- Progress calculation from phase statuses
- Search + sorting
- Project priority
- Dashboard sections
- Stale project highlight
- Soft delete (`is_deleted` flag)
- Multi-note system per project (`task_notes`)
- Per-user flash notes
- Project detail drawer with knowledge preview mode
- Mobile-adaptive layout

## 5. Main API Endpoints

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/auth/logout`
- `GET /api/tasks`
- `GET /api/tasks/{id}`
- `POST /api/tasks`
- `PUT /api/tasks/{id}`
- `DELETE /api/tasks/{id}` (soft delete)
- `POST /api/tasks/{id}/notes`
- `GET /api/tasks/{id}/shares`
- `POST /api/tasks/{id}/shares`
- `PUT /api/tasks/{id}/shares/{shareId}`
- `DELETE /api/tasks/{id}/shares/{shareId}`

Except for register/login, task and flash-note APIs require:

```text
Authorization: Bearer <token>
```

## 6. Deploy to Oracle VM (Non-Docker)

### Backend

1. Install Java 21 and `sqlite3`
2. Upload project to `/opt/task-app`
3. Build and run backend:

```bash
cd /opt/task-app/backend
./gradlew build -x test
nohup ./gradlew bootRun > /opt/task-app/backend/backend.log 2>&1 &
```

For an already initialized production database, use:

```bash
nohup ./gradlew bootRun --args='--spring.sql.init.mode=never' > /opt/task-app/backend/backend.log 2>&1 &
```

### Frontend

Use Nginx or any static server to host `/opt/task-app/frontend`.

### Recommended Production Setup

- Nginx as reverse proxy
- Serve frontend on port 80/443
- Proxy `/api` to Spring Boot (port 8080)
- Keep DB at `/opt/task-app/data/tasks.db`

## 7. Notes

- `spring.sql.init.mode=always` is configured in `application.yml` for first-time schema/sample setup, but day-to-day local or production startup should use `--spring.sql.init.mode=never` after the database exists.
- For migration safety, runtime SQLite migration logic is handled in `SqliteMigrationConfig`.
- If you need to reset to the English sample dataset, re-run:

```bash
./backend/scripts/init_sample_db.sh
```
