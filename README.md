# york-student-events
 
> A centralised event discovery and social platform exclusively for University of York students.
 
[![Version](https://img.shields.io/badge/version-0.2.0-blue.svg)](CHANGELOG.md)
[![Versioning](https://img.shields.io/badge/versioning-semantic-brightgreen.svg)](https://semver.org)
[![Code Style](https://img.shields.io/badge/code%20style-Google%20Java-blue.svg)](https://google.github.io/styleguide/javaguide.html)
[![Python](https://img.shields.io/badge/python-3.11+-yellow.svg)](https://www.python.org)
[![Java](https://img.shields.io/badge/java-21+-orange.svg)](https://openjdk.org)
 
---
 
## Overview
 
York Student Events solves a fragmented problem: university events, society meetups, private venue nights, and city-wide activities are scattered across emails, Instagram pages, noticeboards, and separate websites. This platform brings everything onto one hub, visible only to verified University of York students.
 
Beyond event discovery, the platform introduces a cohort-based social layer- think LinkedIn, but stripped back and casual- so a first-year student can find and connect with second-years in the same department, discover mutual interests, and attend events together.
 
---
 
## Features
 
### Planned (future versions)
 
- **Event management** — create, update, and browse events hosted by the university, student societies, private venues, or the city
- **User accounts** — student-verified profiles with cohort metadata (year, department)
- **Subscriptions** — subscribe to event hosts and venues; receive updates when new events are posted
- **Attendance tracking** — mark attendance at events; data feeds into the badge system
- **Badge system** — earn badges based on attendance milestones and event categories
- **Friends system** — send and accept friend requests between student accounts
- **Mutual-interest matching** — algorithm surfaces students with overlapping interests and attendance history
- **Cohort networking** — filter and connect with students by year group and department
- **Web platform** — HTTP-served frontend client
- **Friend network maps** — interactive maps of friend networks
- **Chat system** — messaging with filtering and report/moderation tooling
- **Push notifications** — notify users of updates from subscribed hosts and venues
---
 
## Architecture
 
The backend is to be split across two services:
 
| Service | Language | Responsibility |
|---|---|---|
| `api-core` | Python | Attendance, badges, friend graph, interest matching |
| `event-service` | Java (Spring Boot) | User accounts, subscriptions, event creation, host/venue management, cohort grouping |
 
---
 
## Project Structure
 
```
york-student-events/
│
├── event-service/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── york/
│   │   │   │       └── studentevents/
│   │   │   │           ├── Application.java
│   │   │   │           ├── cohorts/
│   │   │   │           │   ├── ICohort.java
│   │   │   │           │   ├── ICohortRepository.java
│   │   │   │           │   ├── Cohort.java
│   │   │   │           │   ├── CohortService.java
│   │   │   │           │   └── CohortController.java
│   │   │   │           ├── events/
│   │   │   │           │   ├── IEvent.java
│   │   │   │           │   ├── IEventRepository.java
│   │   │   │           │   ├── Event.java
│   │   │   │           │   ├── EventCategory.java
│   │   │   │           │   ├── EventService.java
│   │   │   │           │   ├── UserEventService.java
│   │   │   │           │   └── EventController.java
│   │   │   │           ├── exceptions/
│   │   │   │           │   ├── CapacityExceededException.java
│   │   │   │           │   ├── CohortNotFoundException.java
│   │   │   │           │   ├── EventNotFoundException.java
│   │   │   │           │   ├── UserNotFoundException.java
│   │   │   │           │   └── VenueNotFoundException.java
│   │   │   │           ├── users/
│   │   │   │           │   ├── IUser.java
│   │   │   │           │   ├── IUserRepository.java
│   │   │   │           │   ├── User.java
│   │   │   │           │   ├── UserService.java
│   │   │   │           │   └── UserController.java
│   │   │   │           ├── venues/
│   │   │   │           │   ├── IVenue.java
│   │   │   │           │   ├── IVenueRepository.java
│   │   │   │           │   ├── Venue.java
│   │   │   │           │   ├── VenueService.java
│   │   │   │           │   └── VenueController.java
│   │   │   │           ├── subscriptions/
│   │   │   │           │   ├── IObserver.java
│   │   │   │           │   ├── IObservable.java
│   │   │   │           │   └── EventNotificationService.java
│   │   │   │           ├── subprocess/
│   │   │   │           │   ├── RequestType.java
│   │   │   │           │   ├── IPayload.java
│   │   │   │           │   ├── UserIdPayload.java
│   │   │   │           │   ├── AwardBadgePayload.java
│   │   │   │           │   ├── SubprocessRequestFactory.java   # Java→Python: spawns api-core
│   │   │   │           │   └── SubprocessResponder.java        # Python→Java: entry point for api-core
│   │   │   │           └── repository/
│   │   │   │               ├── IEntity.java
│   │   │   │               ├── IRepository.java
│   │   │   │               └── inmemory/
│   │   │   │                   ├── AbstractInMemoryRepository.java
│   │   │   │                   ├── InMemoryCohortRepository.java
│   │   │   │                   ├── InMemoryEventRepository.java
│   │   │   │                   ├── InMemoryUserRepository.java
│   │   │   │                   └── InMemoryVenueRepository.java
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/
│   │       └── java/
│   │           └── york/
│   │               └── studentevents/
│   │                   ├── ApplicationTests.java
│   │                   ├── events/
│   │                   │   ├── EventServiceTest.java
│   │                   │   └── EventTest.java
│   │                   ├── users/
│   │                   │   └── UserServiceTest.java
│   │                   ├── subscriptions/
│   │                   │   └── EventNotificationServiceTest.java
│   │                   └── subprocess/
│   │                       ├── RequestTypeTest.java
│   │                       ├── PayloadTest.java
│   │                       ├── SubprocessRequestFactoryTest.java
│   │                       ├── SubprocessRequestFactoryPathTest.java
│   │                       ├── SubprocessRequestFactoryIntegrationTest.java
│   │                       └── SubprocessResponderTest.java
│   └── pom.xml
│
├── api-core/
│   ├── src/
│   │   ├── attendance/
│   │   │   └── attendance.py
│   │   ├── badges/
│   │   │   └── badges.py
│   │   ├── bridge/                 # subprocess bridge to event-service
│   │   │   ├── __init__.py
│   │   │   ├── client.py           # Python→Java: spawns SubprocessResponder
│   │   │   └── responder.py        # Java→Python: handler factory (stubbed)
│   │   ├── friends/
│   │   │   └── getFriendCircle.py
│   │   ├── matching/
│   │   │   └── matching.py
│   │   └── repositories/           # in-memory repository pattern (mirrors Java)
│   │       ├── __init__.py
│   │       └── base.py
│   └── tests/
│       ├── conftest.py
│       ├── test_attendance.py
│       ├── test_badges.py
│       ├── test_bridge_client.py
│       ├── test_bridge_responder.py
│       ├── test_bridge_integration.py
│       ├── test_friends.py
│       └── test_matching.py
│
├── docs/
│   ├── api-spec.yaml
│   ├── apidocs/              # generated Javadoc (mvn package / javadoc:javadoc)
│   └── docs/
│       └── subprocess-contract.md   # Python↔Java JSON envelope contract
│
├── .github/
│   ├── ISSUE_TEMPLATE/
│   │   ├── feature.md
│   │   └── bug.md
│   ├── workflows/
│   │   ├── build.yml
│   │   ├── lint.yml
│   │   ├── claude.yml
│   │   ├── move-to-in-review.yml
│   │   └── manage-blocked-label.yml
│   └── pull_request_template.md
│
├── .gitignore
├── pytest.ini
├── CHANGELOG.md
├── CLAUDE.md
├── LICENSE
└── README.md
```
 
---
 
## Getting Started
 
### Prerequisites
 
- Python 3.11+
- Java 21+

### Running api-core (Python)

```bash
# From the repo root — no requirements file yet
python -m pytest api-core/tests/
```

### Running event-service (Java / Maven)

```bash
cd event-service
./mvnw spring-boot:run        # run the service
./mvnw test                   # run tests
./mvnw javadoc:javadoc        # generate Javadoc into docs/apidocs/
```
 
---
 
## Versioning
 
This project uses [Semantic Versioning](https://semver.org/). Releases follow the `MAJOR.MINOR.PATCH` format:
 
- `MAJOR` — breaking API changes
- `MINOR` — new backwards-compatible features
- `PATCH` — backwards-compatible bug fixes
See [CHANGELOG.md](CHANGELOG.md) for the full release history.
 
- **Breaking changes** are flagged with `!` (e.g. `feat!:`, `fix!:`). A breaking change must have a corresponding issue opened first.

---

## Contributing

Contributions are welcome from University of York students and staff. Please open an issue before submitting a pull request so the proposed change can be discussed first.

This project uses a milestone-based branching model. Work flows from short-lived
development branches up through per-milestone stable branches into `main`:

```
<milestone>/<label>/<name>  ──PR──▶  stable-<milestone>  ──PR──▶  main
```

### Branching

- **Development branches** — `<milestone>/<label>/<name>`, where `<label>` is a
  Conventional Commit type (`feat`, `fix`, `refactor`, `docs`, `chore`, …). Version
  milestones replace dots with hyphens (`v1.0.0` → `v1-0-0`).
  - `m1/feat/irepository` — defining the `IRepository` interface in milestone M1
  - `v1-0-0/refactor/consoleview` — refactoring the console view in milestone v1.0.0
- **Stable branches** — `stable-<milestone>` (e.g. `stable-m1`, `stable-v1-0-0`).
  Development branches are merged here via pull request once ready.
- **`main`** — a completed milestone is merged from its stable branch into `main`
  via a further pull request.
 
### Pull requests

1. Open an issue describing the change before starting work.
2. Branch from the relevant stable branch using the naming convention above.
3. Open a pull request targeting the appropriate branch (development → `stable-<milestone>`;
   completed milestone → `main`).
4. Every pull request must pass the automated checks (build, test, lint) before it can be merged.

### Commits & versioning

- **[Conventional Commits](https://www.conventionalcommits.org/)** (`feat:`, `fix:`,
  `refactor:`, `chore:`, `docs:`, …).
- **Breaking changes** are flagged with `!` (e.g. `feat!:`, `fix!:`). A breaking change must have a corresponding issue opened first.
- Releases follow **[Semantic Versioning](https://semver.org/)**.

Commit messages should follow the [Conventional Commits](https://www.conventionalcommits.org/) specification. Breaking changes should be marked using an exclamation mark (`!`) after the type/scope, before the colon — e.g. `fix!:` or `feat!:`.
