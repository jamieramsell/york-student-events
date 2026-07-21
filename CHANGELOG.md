# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [0.4.0] - 2026-07-21

M4 - Gamification: attendance tracking, the badge system, and automatic badge evaluation driven by user activity, plus the cross-service plumbing needed to support them and an explicit composition root for `api-core`. Still pure business logic, with no HTTP layer or real persistence yet.

### Added
- **Attendance (`api-core`):** `attendance` package provides an `Attendance` record (extends `IEntity`), `AttendanceService` (record / withdraw / `has_attended` / query a user's attendances and an event's attendees) injected with `InMemoryAttendanceRepository`
- **Activity events (`api-core`):** `activity` package provides a lightweight in-process publish/subscribe registry so state changes (e.g. recording an attendance) can trigger downstream reactions such as badge evaluation without direct coupling
- **Badges (`api-core`):** fully built `badges` slice, which provides `Badge` and `AwardedBadge` entities with in-memory repositories; a composable predicate DSL for award conditions (`predicates.py`: `IPredicate` + And/Or/Not combinators and `Min*` leaves, with JSON (de)serialisation via `predicate_from_dict`); `BadgeService` (create / award / revoke / query plus condition-driven `evaluate_badges`); and `EvaluationService`, which builds an `AwardContext` from the other slices and auto-awards newly earned badges
- **Automatic badge evaluation:** `EvaluationService` subscribes to the `activity` registry, so a user's badges are re-evaluated whenever their activity changes, notifying event-service of each newly awarded badge over the bridge
- **Recommendations (`api-core`):** `matching` reworked into a `recommendations` package (`recommendations/base.py`)
- **Composition root (`api-core`):** `bootstrap.py` assembles the whole service graph via constructor injection; services now receive their repositories and collaborators as dependencies rather than reaching for module-level singletons
- **Subprocess bridge:** new `GET_EVENT_INFO`, `GET_BATCH_EVENT_INFO`, and `BADGE_AWARDED` request types (Java `SubprocessResponder` handlers + Python `bridge.client` callers).
- Comprehensive unit and integration tests: Python (`test_attendance.py`, `test_activity.py`, `test_badges.py`, `test_evaluation.py`, `test_recommendations.py` / `test_recommendations_integration.py`, expanded bridge suites); Java (expanded `SubprocessResponderTest`, `SubprocessRequestFactoryTest` / `SubprocessRequestFactoryIntegrationTest`, `PayloadTest`, `RequestTypeTest`)

### Changed
- **Breaking:** the `api-core` service layer now uses explicit dependency injection — repositories and collaborating services are constructor-injected via `bootstrap`, replacing the previous module-level `_repository` singletons
- Renamed the `matching` package and its tests to `recommendations`
- Updated `README.md` and `CLAUDE.md` files to reflect the program's current state

### Removed
- Removed the committed generated Javadoc site (`docs/apidocs/`) from version control; it is build output and is regenerated on demand via `./mvnw javadoc:javadoc`

### Docs
- Added `docs/adr/0001-inter-service-transport.md`, an ADR recording the decision to use the per-call subprocess bridge as the inter-service transport
- Documented `GET_EVENT_INFO`, `GET_BATCH_EVENT_INFO`, and `BADGE_AWARDED` in `docs/subprocess-contract.md`

### Known Issues
- **#205 — attendance is not rolled back when badge evaluation fails (deferred to v0.4.1):** because `EvaluationService` runs synchronously inside `activity.publish`, a failed cross-service event lookup during badge evaluation propagates back out of `record_attendance`, so the caller sees an `error` for an attendance that was in fact saved (and a retry then reports a duplicate). This is **not fixed in 0.4.0**; it is scheduled for a **v0.4.1** patch so that M5 (v0.5.0) development can proceed in parallel on `main`.

[v0.4.0]: https://github.com/jamieramsell/york-student-events/releases/tag/v0.4.0

## [0.3.0] - 2026-07-04

M3 - Social & Notification Layer: the friend graph, friend-based event recommendations, a polymorphic user model (students vs. hosts), and event subscriptions with Observer-pattern notification delivery. Still pure business logic — no HTTP layer or real persistence.

### Added
- **Friends (`api-core`):** `friends` package with the `Friendship` frozen dataclass (extends `IEntity`) and `FriendshipStatus` enum in `base.py`, `FriendshipService` (send / accept / delete friend requests) injected with a repository, and `InMemoryFriendshipRepository` (dict-backed, keyed by the `frozenset` of the two user UUIDs); package `__init__.py` re-exports the public surface via `__all__`
- **Interest matching (`api-core`):** `get_recommended_events()` in `matching.py`, surfacing events from a user's friends (calls into the Java service across the subprocess bridge)
- **Polymorphic user model (`event-service`):** abstract `User` base with concrete `Student` and `Host` classes, `IStudent` / `IHost` interfaces, and an `IUser.UserType` enum exposed via `getType()`
- **Subscriptions & Observer (`event-service`):** `IObserver` / `IObservable` contracts, `Subscription` entity (`ISubscription`), `ISubscriptionRepository` + `InMemorySubscriptionRepository`, `NotificationType` enum, `UserEventObserver`, `EventNotificationService` (implements `IObservable`), and `SubscriptionService` coordinating records and notification broadcast
- `StudentEventService` now auto-subscribes/unsubscribes users on register/deregister and broadcasts a notification when an event reaches its 80% capacity threshold; `subscribeToEvent()` / `unsubscribeFromEvent()` methods
- `UserNotAuthorisedException` (unchecked) — thrown when a user attempts an operation not permitted for their role
- `CohortService.removeStudentFromCohort()`
- Subprocess bridge: `GET_RECOMMENDED_EVENTS` request type, `SubprocessRequestFactory.buildGetRecommendedEvents()`, and the matching Python responder handler
- Comprehensive unit and integration tests: Python (`test_friends.py`, `test_matching.py`, `test_matching_integration.py`); Java (`StudentEventServiceTest`, `EventSubscriptionIntegrationTest`, `InMemorySubscriptionRepositoryTest`, `SubscriptionServiceTest`, `SubscriptionTest`, `UserEventObserverTest`, expanded `EventNotificationServiceTest`)

### Changed
- **Breaking:** `Friendship` moved from `getFriendCircle.py` into `friends/base.py`; friendships are now keyed by user `UUID`s rather than `int`s
- **Breaking:** `IUser` stripped of student-specific fields and methods; `User` is now abstract, with student behaviour living in `Student` and the `UserType` enum distinguishing roles
- **Breaking:** a user's registered events are stored as a `Set` rather than a `List`; `CohortService` and `StudentEventService` operate on `Student` rather than `User`
- **Breaking:** renamed `UserEventService` to `StudentEventService`
- **Breaking:** Python bridge `get_user_events()` now takes a `UUID` and returns a list of `UUID`s (previously `str` → `list[str]`)
- Bumped `pom.xml` project version to `0.3.0`; updated `README.md` and `CLAUDE.md`

### Docs
- Moved `subprocess-contract.md` out of the nested `docs/docs/` folder up to `docs/`, and documented the `GET_RECOMMENDED_EVENTS` request in it
- Regenerated the Javadoc site under `docs/apidocs/`

[v0.3.0]: https://github.com/jamieramsell/york-student-events/releases/tag/v0.3.0

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

[v0.2.0]: https://github.com/jamieramsell/york-student-events/releases/tag/v0.2.0

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
