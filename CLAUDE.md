# CLAUDE.md

## Project Overview

**york-student-events** is a centralised event discovery and social platform for University of York students. It is still pre-release: the domain models and core business logic across both services are now implemented (attendance, badges, friend graph, recommendations; events, users, venues, cohorts, subscriptions), but there is no live HTTP layer or real persistence yet, controllers remain thin and unwired, and both services run against in-memory repositories.

## Architecture

Two backend services:

| Service | Language / Stack | Responsibility |
|---|---|---|
| `api-core` | Python 3.11+ | Attendance tracking, badges, friend graph, interest matching |
| `event-service` | Java 21 / Spring Boot 3.x | Users, events, venues, subscriptions |

No frontend exists yet. No database; both services use in-memory repositories.

The two services talk to each other over a **subprocess bridge** — a JSON-over-stdio contract where one service spawns the other as a child process per call. api-core's `bridge/client.py` spawns the Java `SubprocessResponder`; api-core's `bridge/responder.py` answers requests issued by event-service. The envelope contract is documented in `docs/subprocess-contract.md`. Stdlib/standard-library only, in keeping with the no-dependencies convention.

## Running the Services

### api-core (Python)
```bash
# From repo root — no requirements file yet
python -m pytest api-core/tests/
```

### event-service (Java / Maven)
```bash
cd event-service
./mvnw spring-boot:run        # run the service
./mvnw test                   # run tests
./mvnw javadoc:javadoc        # generate Javadoc into docs/apidocs/ (gitignored build output)
./mvnw verify                 # compile, test, and run Checkstyle (Google Java Style)
```

## Conventions

- **Commits**: Conventional Commits (`feat:`, `fix:`, `chore:`, etc.). Breaking changes are marked using `<type>!:` (e.g. `fix!:`)
- **Versioning**: Semantic Versioning — see `CHANGELOG.md`
- **Branching**: feature branches (`feat/your-feature`), open an issue before PRing
- **Java package root**: `york.studentevents`
- **Python tests**: pytest, co-located in `api-core/tests/`

## Code Style

### Java (`event-service`)

- **Interfaces**: prefix with `I` — e.g. `IEvent`, `IObserver`, `IRepository`
- **Classes**: PascalCase — e.g. `EventNotificationService`, `InMemoryEventRepository`
- **Methods / variables**: camelCase (standard Java)
- **In-memory implementations**: named `InMemory{Entity}Repository`, placed under `repository/inmemory/`
- **Package layout**: one package per domain slice (`events`, `users`, `venues`, `cohorts`, `subscriptions`, `repository`, `subprocess`); cross-cutting `exceptions` package for custom exception types
- **MVC layering**: `Controller` → `Service` → `Repository`; keep business logic out of controllers
- **Tests**: JUnit 5 via Spring Boot Test; one test class per service class (e.g. `EventServiceTest` for `EventService`)
- **Style enforcement**: code must conform to the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html), enforced by the `maven-checkstyle-plugin` (Checkstyle 10.17.0, `google_checks.xml`) during `mvn verify`

### Python (`api-core`)

- **Files**: snake_case (e.g. `attendance_service.py`, `badge_service.py`)
- **Functions / variables**: snake_case
- **Interfaces**: prefix with `I` — e.g. `IRepository`, `IEntity` (mirrors the Java convention; defined as `abc.ABC` abstract base classes under `repositories/`)
- **Packages**: each domain slice is a package whose `__init__.py` re-exports its public surface via `__all__` (e.g. `friends/`, `badges/`, `bridge/`, `repositories/`)
- **Test files**: prefixed `test_` and co-located in `api-core/tests/` (e.g. `test_attendance.py`)
- **Test runner**: pytest — run from repo root with `python -m pytest api-core/tests/`
- No third-party dependencies yet; avoid adding any without a `requirements.txt`

## Current State

The domain models and service-layer business logic are implemented across both services:
- **event-service:** concrete `Event`, `User` (abstract, with `Student` / `Host`), `Venue`, and `Cohort` entities; their services and in-memory repositories (`AbstractInMemoryRepository` + per-entity subclasses); the subscription / Observer stack; and the Java side of the subprocess bridge.
- **api-core:** the `friends` graph; the `recommendations` slice; the `attendance` slice (`Attendance` record, `AttendanceService`, `InMemoryAttendanceRepository`); the `activity` in-process publish/subscribe registry; and the fully built `badges` slice — `Badge` / `AwardedBadge` entities, their in-memory repositories, a composable predicate DSL for award conditions (`predicates.py`: `IPredicate` + And/Or/Not combinators and `Min*` leaves, with JSON (de)serialisation via `predicate_from_dict`), `badge_service.py` (create/award/revoke/query plus condition-driven `evaluate_badges`), and `EvaluationService`, which subscribes to `activity` to auto-award badges when a user's activity changes. `bootstrap.py` is the composition root that wires the whole graph via constructor injection.

Controllers exist but are thin and unwired (e.g. `EventController` is `@Deprecated`, with no Spring MVC request mappings), so no HTTP endpoints are live yet. `docs/api-spec.yaml` documents the *intended* REST contract ahead of implementation.

The primary established patterns are:
- Repository pattern with in-memory implementations (`InMemoryEventRepository`, `InMemoryBadgeRepository`, …), constructor-injected on both sides — via `bootstrap.py` on the Python side and via constructors on the Java side
- Observer pattern for subscriptions (`IObserver` / `IObservable` / `SubscriptionService`)
- In-process publish/subscribe (`activity`) decoupling state changes from their reactions (e.g. recording an attendance triggers badge evaluation)
- Composite / specification-style predicates for badge award conditions
- Subprocess bridge (JSON-over-stdio) for cross-service calls (`GET_USER_EVENTS`, `GET_EVENT_INFO`, `GET_BATCH_EVENT_INFO`, `BADGE_AWARDED`, …)
- Dependency injection to keep each layer truly separate, with `bootstrap.py` as the api-core composition root
- Spring Boot MVC structure (Controller → Service → Repository)

CI runs on every PR (`build.yml` for the Java build/test/Checkstyle, `lint.yml` for `ruff` on `api-core`). No persistence layer or authentication exists yet; cross-service communication is limited to the per-call subprocess bridge (no long-running RPC or shared database).
