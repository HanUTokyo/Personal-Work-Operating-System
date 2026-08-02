# Changelog

All notable changes to Task App are documented here.

## [1.0.0-beta.3] - 2026-08-02

### Added

- React 19, TypeScript, Vite, Mantine, and TipTap Web application
- Weekly and long-term personal task management
- Global AI suggestions and AI-friendly project export
- Project pinning, hierarchical phases, rich-text knowledge, and expanded note workflows
- English, Simplified Chinese, and Japanese localization
- Light, dark, and system theme support
- Backend repository, controller, service, authentication, and migration tests

### Changed

- Reworked the dashboard, project list, project detail, editing, and sharing experiences
- Expanded the SQLite schema and runtime migration logic for beta3 data models
- Replaced the legacy static Web frontend with a component-based React application
- New passwords use bcrypt; legacy SHA-256 hashes upgrade after successful login
- CORS origins and the SQLite connection URL can be configured through environment variables

### Security and release hygiene

- Existing authentication tokens are invalidated once during the beta3 migration
- Runtime databases, local backups, logs, PID files, signing files, build outputs, and application binaries are excluded from source control
- Web and Backend versions are aligned at `1.0.0-beta.3`

## [1.0.0-beta.1]

- Initial public beta with project progress tracking, sharing, soft deletion, multiple notes, responsive Web layout, and English sample data
