# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [0.2.0] - 2026-06-26

M2 - Core Domain Logic: concrete domain models, the in-memory repository layer, the first service-layer methods, and a bidirectional Python↔Java subprocess bridge. Pure business logic: no HTTP layer or real persistence yet.

### Added
- Concrete domain models: `Event` (with new `EventCategory` enum), `User`, `Venue`, and `Cohort`
- `IEntity` interface abstracting the UUID identifier shared by repository entities
- Repository layer: `AbstractInMemoryRepository` base plus per-domain in-memory repositories (`InMemoryEventRepository`, `InMemoryUserRepository`, `InMemoryVenueRepository`, `InMemoryCohortRepository`)
- Service layer: `UserEventService` and `CohortService`
- Subprocess communication contract (`docs/docs/subprocess-contract.md`) defining strict stdin/stdout JSON envelopes — `request` (`requestType` + `payload`), `success` (`status: "ok"`), and `error` (`status: "error"`)
- Bidirectional subprocess bridge:
  - Java→Python: `RequestType` enum + `SubprocessRequestFactory` (Gson, `ProcessBuilder`) invoking Python's `api-core/src/bridge/responder.py`, which routes requests via `MessageHandlerFactory` (handlers stubbed)
  - Python→Java: `api-core/src/bridge/client.py` spawning the Java `SubprocessResponder` entry point to answer `GET_USER_EVENTS` via `UserEventService`
  - Java request payload models: `IPayload`, `UserIdPayload`, `AwardBadgePayload`
- `api-core` `repositories` package mirroring the Java repository pattern, plus `badges.py` and `getFriendCircle.py` stubs
- JUnit tests for domain/services (`EventTest`) and the subprocess layer (`RequestTypeTest`, `PayloadTest`, `SubprocessRequestFactoryTest`, `SubprocessRequestFactoryPathTest`, `SubprocessRequestFactoryIntegrationTest`, `SubprocessResponderTest`)
- Python bridge tests (`test_bridge_client.py`, `test_bridge_responder.py`, `test_bridge_integration.py`) and `pytest.ini`
- `build.yml` CI workflow

### Changed
- **Breaking:** migrated entity IDs from `long` to UUIDs; `IUser`, `IEvent`, and `IRepository` now operate on `UUID`, with `IRepository<T>` constrained to `T extends IEntity`
- **Breaking:** `IVenue` now supports an optional maximum attendee capacity (`int` → `Integer`) and exposes setter methods for all attributes except ID
- Aligned the Java codebase to Google Java Style (with checkstyle suppressions); updated `pom.xml` (project version `0.2.0`) and `README.md`

### Fixed
- Entity classes no longer regenerate a new UUID on each access

### Docs
- Javadoc site now generated under `docs/apidocs/` (previously `docs/event-service/`)

## [0.1.0] - 2026-06-17

M1 - App Foundation: project scaffolding for both backend services.

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
