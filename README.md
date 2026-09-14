# york-student-events
 
> A centralised event discovery and social platform exclusively for University of York students.
 
[![Version](https://img.shields.io/badge/version-0.5.0-blue.svg)](CHANGELOG.md)
[![Versioning](https://img.shields.io/badge/versioning-semantic-brightgreen.svg)](https://semver.org)
[![Code Style](https://img.shields.io/badge/code%20style-Google%20Java-blue.svg)](https://google.github.io/styleguide/javaguide.html)
[![Python](https://img.shields.io/badge/python-3.12+-yellow.svg)](https://www.python.org)
[![Java](https://img.shields.io/badge/java-21+-orange.svg)](https://openjdk.org)
 
---
 
## Overview
 
York Student Events solves a fragmented problem: university events, society meetups, private venue nights, and city-wide activities are scattered across emails, Instagram pages, noticeboards, and separate websites. This platform brings everything onto one hub, visible only to verified University of York students.
 
Beyond event discovery, the platform introduces a cohort-based social layer- think LinkedIn, but stripped back and casual- so a first-year student can find and connect with second-years in the same department, discover mutual interests, and attend events together.
 
---
 
## Features
 
### Implemented (domain logic — no HTTP layer yet)

The business logic for these is built and tested across both services; none is exposed over HTTP or backed by real persistence yet.

- **Event management** — create, update, and browse events hosted by the university, student societies, private venues, or the city
- **Subscriptions** — subscribe to event hosts and venues; receive updates when new events are posted
- **Attendance tracking** — mark attendance at events; data feeds into the badge system
- **Badge system** — earn badges based on attendance milestones and event categories
- **Friends system** — send and accept friend requests between student accounts
- **Mutual-interest matching** — algorithm surfaces students with overlapping interests and attendance history
- **User accounts** *(partial)* — polymorphic `Student` / `Host` model with cohort metadata; student verification and authentication are not yet implemented
- **Cohort networking** *(partial)* — cohort model and membership exist; filtering and connecting students by year group and department is not yet complete

### Planned (future versions)

- **Web platform** — HTTP-served frontend client
- **Friend network maps** — interactive maps of friend networks
- **Chat system** — messaging with filtering and report/moderation tooling
- **Push notifications** — notify users of updates from subscribed hosts and venues (only in-process Observer notifications exist today)
---
 
## Architecture
 
The backend is split across two services:
 
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
│   │   │   │           │   ├── StudentEventService.java
│   │   │   │           │   └── EventController.java
│   │   │   │           ├── users/
│   │   │   │           │   ├── IUser.java
│   │   │   │           │   ├── IHost.java
│   │   │   │           │   ├── IStudent.java
│   │   │   │           │   ├── IUserRepository.java
│   │   │   │           │   ├── User.java
│   │   │   │           │   ├── Host.java
│   │   │   │           │   ├── Student.java
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
│   │   │   │           │   ├── ISubscription.java
│   │   │   │           │   ├── ISubscriptionRepository.java
│   │   │   │           │   ├── NotificationType.java
│   │   │   │           │   ├── UserEventObserver.java
│   │   │   │           │   ├── EventNotificationService.java
│   │   │   │           │   └── SubscriptionService.java
│   │   │   │           ├── subprocess/
│   │   │   │           │   ├── RequestType.java
│   │   │   │           │   ├── IPayload.java
│   │   │   │           │   ├── UserIdPayload.java
│   │   │   │           │   ├── AwardBadgePayload.java
│   │   │   │           │   ├── SubprocessRequestFactory.java   # Java→Python: spawns api-core
│   │   │   │           │   └── SubprocessResponder.java        # Python→Java: entry point for api-core
│   │   │   │           ├── exceptions/
│   │   │   │           │   ├── CapacityExceededException.java
│   │   │   │           │   ├── CohortNotFoundException.java
│   │   │   │           │   ├── EventNotFoundException.java
│   │   │   │           │   ├── UserNotAuthorisedException.java
│   │   │   │           │   ├── UserNotFoundException.java
│   │   │   │           │   └── VenueNotFoundException.java
│   │   │   │           └── repository/
│   │   │   │               ├── IEntity.java
│   │   │   │               ├── IRepository.java
│   │   │   │               └── inmemory/
│   │   │   │                   ├── AbstractInMemoryRepository.java
│   │   │   │                   ├── InMemoryCohortRepository.java
│   │   │   │                   ├── InMemoryEventRepository.java
│   │   │   │                   ├── InMemorySubscriptionRepository.java
│   │   │   │                   ├── InMemoryUserRepository.java
│   │   │   │                   └── InMemoryVenueRepository.java
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/
│   │       └── java/
│   │           └── york/
│   │               └── studentevents/
│   │                   ├── ApplicationTests.java
│   │                   ├── cohorts/
│   │                   ├── events/
│   │                   ├── repository/
│   │                   ├── subprocess/
│   │                   ├── subscriptions/
│   │                   └── users/
│   └── pom.xml
│
├── api-core/
│   ├── src/
│   │   ├── activity/               # in-process publish/subscribe registry
│   │   ├── attendance/
│   │   ├── badges/
│   │   ├── bridge/                 # subprocess bridge to event-service
│   │   │   ├── __init__.py
│   │   │   ├── client.py           # Python→Java: spawns SubprocessResponder
│   │   │   └── responder.py        # Java→Python: handler factory (stubbed)
│   │   ├── friends/
│   │   ├── recommendations/
│   │   ├── repositories/           # in-memory repository pattern (mirrors Java)
│   │   │   ├── __init__.py
│   │   │   └── base.py
│   │   └── bootstrap.py            # composition root: wires the service graph
│   └── tests/
│       ├── conftest.py
│       ├── test_activity.py
│       ├── test_attendance.py
│       ├── test_badges.py
│       ├── test_bridge_client.py
│       ├── test_bridge_responder.py
│       ├── test_bridge_integration.py
│       ├── test_evaluation.py
│       ├── test_friends.py
│       ├── test_recommendations.py
│       └── test_recommendations_integration.py
│
├── docs/
│   ├── api-spec.yaml
│   ├── adr/                         # architecture decision records
│   └── subprocess-contract.md       # Python↔Java JSON envelope contract
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
 
- Python 3.12+
- Java 21+
- Docker (for the local development database)

### Database (local development)

Both services share a single PostgreSQL instance with clear table ownership (event-service owns users, events, venues, cohorts; api-core owns friendships, badges, attendance). Each service owns and migrates its own tables (event-service with Flyway, api-core with Alembic) as described under their run sections below. See [ADR-0002](docs/adr/0002-database-technology.md) for the rationale.

First, create your local environment file from the template:

```bash
cp .env.example .env   # then edit credentials if you wish; .env is gitignored
```

Then start Postgres with a single command:

```bash
docker compose -f docker-compose.db.yml up -d
```

| Setting  | Default value           | Variable            |
|----------|-------------------------|---------------------|
| Host     | `localhost`             | `DB_HOST`           |
| Port     | `5432`                  | `DB_PORT`           |
| Database | `york_student_events`   | `DB_NAME` / `POSTGRES_DB`     |
| User     | `yse`                   | `DB_USERNAME` / `POSTGRES_USER` |
| Password | _(set in your `.env`)_  | `DB_PASSWORD` / `POSTGRES_PASSWORD` |

Stop the database with `docker compose -f docker-compose.db.yml down`, or
`down -v` to also wipe its data.

> **Port `5432` already in use?** Set `POSTGRES_PORT` and `DB_PORT` to a free port (e.g. `5433`) in your `.env`, and change the port in `DB_URL` and `DATABASE_URL` to match, as all four must agree. Note that postgres bakes the port into the container on the first `up`, so if the container already exists, recreate it by running `down -v`, followed by `up -d`.

### Running api-core (Python)

```bash
# Install dependencies (SQLAlchemy Core, psycopg, Alembic)
pip install -r api-core/requirements.txt

# Run the tests (from the repo root; these use in-memory SQLite, no database needed)
python -m pytest api-core/tests/
```

api-core reads its connection URL from `DATABASE_URL` (already in `.env.example`) and manages its schema with Alembic. With the database running (see above) and your `.env` exported, create the tables and verify connectivity:

```bash
set -a; . .env; set +a       # export DATABASE_URL
cd api-core
alembic upgrade head         # create the friendships / badges / awarded_badges / attendance tables
python scripts/check_db.py   # smoke test: connect, write and read back a row → prints OK
```

When you change the schema in `repositories/sql/schema.py`, generate a migration
for it and review the generated file before applying it:

```bash
cd api-core
alembic revision --autogenerate -m "describe the change"   # writes a new file under alembic/versions/
# open the generated migration and check its upgrade()/downgrade(), then:
alembic upgrade head                                       # apply it
```

Autogenerate reliably detects added and dropped tables and columns, but renames, type changes, and `CHECK` constraints usually need hand-editing, so you must always review the generated migration for missing details.

### Running event-service (Java / Maven)

`./mvnw test` and `./mvnw verify` run against an in-memory H2 database and need no setup. **Running the service** connects to Postgres, so start the database (see [Database (local development)](#database-local-development) above) and export your `.env` first:

```bash
set -a; . ./.env; set +a      # export DB_URL / DB_USERNAME / DB_PASSWORD
cd event-service
./mvnw spring-boot:run        # run the service (needs Postgres running)
./mvnw test                   # run tests (H2 in-memory, no database needed)
./mvnw javadoc:javadoc        # generate Javadoc into docs/apidocs/
```

> On zsh, source the file as `. ./.env` (with the `./`). A bare `. .env` makes zsh search `$PATH` rather than the current directory and fails with `no such file or directory`.

#### Database schema (Flyway migrations)

event-service owns its tables (users, events, venues, cohorts, and their membership join tables), and manages them with **Flyway**, the Java-side counterpart to api-core's Alembic. Migration scripts live in [`event-service/src/main/resources/db/migration`](event-service/src/main/resources/db/migration) and follow Flyway's `V{n}__description.sql` naming.

Flyway applies any pending migrations **automatically on startup**, before Hibernate validates the entities against the resulting schema (`spring.jpa.hibernate.ddl-auto=validate`). The migration SQL is therefore the single source of truth for the schema; the JPA entities must agree with it, not the other way around. Test runs are an exception, as they use an in-memory H2 database, meaning they don't need an external Postgres.

To change the schema, hand-write the next migration (Flyway has no autogenerate, unlike Alembic) as a new `V{n+1}__….sql` file, where it will be applied on the next startup. Confirm what has run by reading Flyway's history table:

```bash
docker exec york-student-events-db \
  psql -U yse -d york_student_events \
  -c "SELECT version, description, success FROM flyway_schema_history;"
```

Once a migration has been applied to any database, **never edit it**. Flyway records a checksum, and a changed file fails validation on the next run, therefore you must add a new migration instead. Note that a migration you are still drafting locally, which hasn't yet applied anywhere, is still fine to edit.

The Flyway Maven plugin (`./mvnw flyway:info`, `flyway:migrate`) is also available for driving migrations outside the app. Unlike the runtime, it does **not** read `application.properties`, so pass the connection explicitly via the `FLYWAY_URL` / `FLYWAY_USER` / `FLYWAY_PASSWORD` environment variables (or `-Dflyway.url=…` flags).
 
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

This project uses a milestone-based branching model. Work flows from short-lived development branches up through per-milestone stable branches into `main`:

```
<milestone>/<label>/<name>  ──PR──▶  stable-<milestone>  ──PR──▶  main
```

### Branching

- **Development branches** — `<milestone>/<label>/<name>`, where `<label>` is a Conventional Commit type (`feat`, `fix`, `refactor`, `docs`, `chore`, …). Version milestones replace dots with hyphens (`v1.0.0` → `v1-0-0`).
  - `m1/feat/irepository` — defining the `IRepository` interface in milestone M1
  - `v1-0-0/refactor/consoleview` — refactoring the console view in milestone v1.0.0
- **Stable branches** — `stable-<milestone>` (e.g. `stable-m1`, `stable-v1-0-0`). Development branches are merged here via pull request once ready.
- **`main`** — a completed milestone is merged from its stable branch into `main` via a further pull request.
 
### Pull requests

1. Open an issue describing the change before starting work.
2. Branch from the relevant stable branch using the naming convention above.
3. Open a pull request targeting the appropriate branch (development → `stable-<milestone>`; completed milestone → `main`).
4. Every pull request must pass the automated checks (build, test, lint) before it can be merged.

### Commits & versioning

- **[Conventional Commits](https://www.conventionalcommits.org/)** (`feat:`, `fix:`, `refactor:`, `chore:`, `docs:`, …).
- **Breaking changes** are flagged with `!` (e.g. `feat!:`, `fix!:`). A breaking change must have a corresponding issue opened first.
- Releases follow **[Semantic Versioning](https://semver.org/)**.

Commit messages should follow the [Conventional Commits](https://www.conventionalcommits.org/) specification. Breaking changes should be marked using an exclamation mark (`!`) after the type/scope, before the colon — e.g. `fix!:` or `feat!:`.
