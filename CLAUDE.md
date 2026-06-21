# CLAUDE.md

## Project Overview

**york-student-events** is a centralised event discovery and social platform for University of York students. It is in early development — most implementation files are stubs.

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
│   │   ├── friends/        (base, friendship_service, friendship_repository, getFriendCircle)
│   │   ├── matching/matching.py
│   │   └── repositories/   (base — IEntity, IRepository)
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
│   │   ├── subscriptions/ (EventNotificationService, IObserver, IObservable)
│   │   └── repository/inmemory/
│   └── pom.xml
└── docs/api-spec.yaml
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
- **Package layout**: one package per domain slice (`events`, `users`, `venues`, `subscriptions`, `repository`)
- **MVC layering**: `Controller` → `Service` → `Repository`; keep business logic out of controllers
- **Tests**: JUnit 5 via Spring Boot Test; one test class per service class (e.g. `EventServiceTest` for `EventService`)

### Python (`api-core`)

- **Files**: snake_case (e.g. `attendance.py`, `matching.py`)
- **Functions / variables**: snake_case
- **Interfaces**: prefix with `I` — e.g. `IRepository`, `IEntity` (mirrors the Java convention; defined as `abc.ABC` abstract base classes under `repositories/`)
- **Packages**: each domain slice is a package whose `__init__.py` re-exports its public surface via `__all__` (e.g. `friends/`, `repositories/`)
- **Test files**: prefixed `test_` and co-located in `api-core/tests/` (e.g. `test_attendance.py`)
- **Test runner**: pytest — run from repo root with `python -m pytest api-core/tests/`
- No third-party dependencies yet; avoid adding any without a `requirements.txt`

## Current State

Most source files are empty stubs. The primary implemented patterns are:
- Observer pattern for subscriptions (`IObserver` / `IObservable` / `EventNotificationService`)
- Repository pattern with in-memory implementations (`InMemoryEventRepository`, `InMemoryUserRepository`)
- Spring Boot MVC structure (Controller → Service → Repository)

No persistence layer, authentication, or inter-service communication exists yet.
