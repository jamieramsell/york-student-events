# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [0.1.0] - 2026-06-19

First milestone (M0) release. Establishes the `event-service` (Java / Spring
Boot) foundation with the Event domain, an end-to-end read endpoint, in-memory
persistence, and automated build, test, and style-guide enforcement.

### Added
- `event-service` (Java 21 / Spring Boot 3.x) scaffolding with `events`, `users`, `venues`, `subscriptions`, `cohorts`, `exceptions`, and `repository` packages
- Domain interfaces: `IEvent`, `IUser`, `IVenue`, `ICohort`, generic `IRepository<T>` and its per-domain extensions
- Custom exception types (`EventNotFoundException`, `UserNotFoundException`, `VenueNotFoundException`, `CohortNotFoundException`, `CapacityExceededException`)
- `api-core` (Python) data structures: `Badge`, `Friendship`
- OpenAPI design spec for `event-service` under `docs/api-spec.yaml`
- Generated Javadoc published under `docs/apidocs/`

[v0.1.0]: https://github.com/jamieramsell/york-student-events/releases/tag/v0.1.0

## [v1.0.0-alpha] - 2026-06-18

First milestone (M0) release. Establishes the `event-service` (Java / Spring
Boot) foundation with the Event domain, an end-to-end read endpoint, in-memory
persistence, and automated build, test, and style-guide enforcement.

### Added

- **Spring Boot `event-service`** — Maven + Spring Boot 3.x project targeting
  Java 21, with the MVC layering (`Controller` → `Service` → `Repository`).
- **Event domain** - `IEvent` interface and concrete `Event` class, including
  start/end date accessors.
- **`GET /events` endpoint** — `EventController` exposes all events, backed by
  `EventService.getAllEvents()`.
- **In-memory persistence** — `IEventRepository` interface and
  `InMemoryEventRepository` implementation, seeded with hardcoded events.
- **Google Java Style Guide enforcement** — `maven-checkstyle-plugin`
  (Checkstyle 10.17.0) runs against the bundled `google_checks.xml` during
  `verify`, with project-specific suppressions layered on top.
- **CI workflows** — `build.yml` compiles, tests, and lints the Java service
  via `./mvnw -B verify`; `lint.yml` runs `ruff` against `api-core`.
- **Generated API docs** — Javadoc HTML published under `docs/apidocs/`.

[v1.0.0-alpha]: https://github.com/jamieramsell/york-student-events/releases/tag/v1.0.0-alpha
