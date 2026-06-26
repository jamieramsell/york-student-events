# CLAUDE.md

## Project Overview

**york-student-events** is a centralised event discovery and social platform for University of York students. It is in early development: the domain interfaces, exception types, and core patterns are defined, but most concrete classes (entities and controllers) are still stubs.

## Architecture

Two backend services:

| Service | Language / Stack | Responsibility |
|---|---|---|
| `api-core` | Python 3.11+ | Attendance tracking, badges, friend graph, interest matching |
| `event-service` | Java 21 / Spring Boot 3.x | Users, events, venues, subscriptions |

No frontend exists yet. No database — both services use in-memory repositories.

## Repo Structure

```
york-student-events/
├── api-core/
│   ├── src/
│   │   ├── attendance/attendance.py
│   │   ├── badges/badges.py
│   │   ├── friends/getFriendCircle.py
│   │   └── matching/matching.py
│   └── tests/
│       ├── test_attendance.py
│       ├── test_badges.py
│       ├── test_friends.py
│       └── test_matching.py
├── event-service/
│   ├── src/main/java/york/studentevents/
│   │   ├── Application.java
│   │   ├── events/        (Event, EventService, EventController, IEvent, IEventRepository)
│   │   ├── users/         (User, UserService, UserController, IUser, IUserRepository)
│   │   ├── venues/        (Venue, VenueService, VenueController, IVenue, IVenueRepository)
│   │   ├── cohorts/       (ICohort, ICohortRepository)
│   │   ├── exceptions/    (EventNotFoundException, UserNotFoundException,
│   │   │                   VenueNotFoundException, CohortNotFoundException,
│   │   │                   CapacityExceededException)
│   │   ├── subscriptions/ (EventNotificationService, IObserver, IObservable)
│   │   └── repository/    (IRepository, inmemory/InMemoryEventRepository,
│   │                       inmemory/InMemoryUserRepository)
│   └── pom.xml
└── docs/
    ├── api-spec.yaml      (OpenAPI design spec for event-service)
    └── event-service/     (generated Javadoc — ./mvnw javadoc:javadoc)
```

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
./mvnw javadoc:javadoc        # generate Javadoc into target/reports/apidocs
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
- **Package layout**: one package per domain slice (`events`, `users`, `venues`, `cohorts`, `subscriptions`, `repository`); cross-cutting `exceptions` package for custom exception types
- **MVC layering**: `Controller` → `Service` → `Repository`; keep business logic out of controllers
- **Tests**: JUnit 5 via Spring Boot Test; one test class per service class (e.g. `EventServiceTest` for `EventService`)
- **Style enforcement**: code must conform to the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html), enforced by the `maven-checkstyle-plugin` (Checkstyle 10.17.0, `google_checks.xml`) during `mvn verify`

### Python (`api-core`)

- **Files**: snake_case (e.g. `attendance.py`, `matching.py`)
- **Functions / variables**: snake_case
- **Test files**: prefixed `test_` and co-located in `api-core/tests/` (e.g. `test_attendance.py`)
- **Test runner**: pytest — run from repo root with `python -m pytest api-core/tests/`
- No third-party dependencies yet; avoid adding any without a `requirements.txt`

## Current State

Domain interfaces (`IEvent`, `IUser`, `IVenue`, `ICohort`), the repository
contracts, and the custom exception types are defined. Concrete entity classes
(`Event`, `User`, `Venue`) and the controllers are still stubs, so no HTTP
endpoints are live yet — `docs/api-spec.yaml` documents the *intended* contract
ahead of implementation. The primary established patterns are:
- Observer pattern for subscriptions (`IObserver` / `IObservable` / `EventNotificationService`)
- Repository pattern with in-memory implementations (`InMemoryEventRepository`, `InMemoryUserRepository`)
- Dependency injection to ensure each layer remains truly seperate
- Spring Boot MVC structure (Controller → Service → Repository)

CI runs on every PR (`build.yml` for the Java build/test/Checkstyle, `lint.yml`
for `ruff` on `api-core`). No persistence layer, authentication, or
inter-service communication exists yet.
