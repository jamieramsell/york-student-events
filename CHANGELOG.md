# Changelog
All notable changes to this project will be documented in this file.
 
The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [0.1.0] - 2026-06-17

M1 - App Foundation: project scaffolding for both backend services.

### Added
- `event-service` (Java 21 / Spring Boot 3.x) scaffolding with `events`, `users`, `venues`, `subscriptions`, `cohorts`, `exceptions`, and `repository` packages
- Domain interfaces: `IEvent`, `IUser`, `IVenue`, `ICohort`, generic `IRepository<T>` and its per-domain extensions
- Custom exception types (`EventNotFoundException`, `UserNotFoundException`, `VenueNotFoundException`, `CohortNotFoundException`, `CapacityExceededException`)
- `api-core` (Python) data structures: `Badge`, `Friendship`
- Generated Javadoc published under `docs/apidocs/`
