# ADR-0002: Database technology and local development instance

**Status:** Accepted
**Date:** 2026-07-23
**Deciders:** Jamie Ramsell

## Context

M5 (Persistence Layer) replaces the in-memory repositories on both sides
(`InMemory*Repository` in event-service, the in-memory dicts behind api-core's
`repositories/`) with durable storage. Before any persistence code is written
(issues #88 and #95, both blocked by this one) we need to agree on:

1. Which database technology to use.
2. Do the two services share one database, or each own their own?
3. How a developer runs it locally, reproducibly, with no hand setup.

ADR-0001 already assumes a shared database when reasoning about what
persistence fixes for the subprocess bridge, so this ADR makes that assumption
explicit and records the technology and local-dev mechanics around it.

## Decision drivers

- Standard, production-realistic stack for a portfolio project.
- Works well with both Spring Data JPA (Java) and Python (SQLAlchemy + psycopg).
- Reproducible for every team member with a single command
- Light enough to run for a student project; not full microservice
  infrastructure.

## Decisions

### 1. Technology: PostgreSQL 16

PostgreSQL is the default production relational database, has first-class
support in both Spring Data JPA and the Python ecosystem, and demonstrates a
realistic stack. Tests do not hit Postgres, as event-service uses H2 in test
scope (see issue #88), and api-core's approach to test isolation is settled in
#95.

### 2. Topology: one shared database, clear table ownership

Both services connect to a single Postgres instance. Ownership is by service and
enforced by convention, not by cross-table access:

- **event-service owns:** users, events, venues, cohorts.
- **api-core owns:** friendships, badges, attendance.

Neither service reads or writes the other's tables. Cross-service data continues
to flow over the subprocess contract (`docs/subprocess-contract.md`), never by
reaching into another service's tables.

Using a separate database per service has also been considered, which is
stricter in a pure microservices sense, but doubles the local-dev footprint and
operational surface for no real benefit at this project's scale.
Shared-with-ownership keeps isolation where it matters (schemas, no cross-table
writes) while also staying cheap to run. If a service is ever extracted, its
tables are already cleanly delineated and can be split out.

### 3. Local development: Docker Compose

A throwaway, database-only compose file (`docker-compose.db.yml`) starts an
identical Postgres for anyone with one command:

```bash
docker compose -f docker-compose.db.yml up -d
```

This is preferred over a hand-installed local server (e.g. Postgres.app /
Homebrew) because it is reproducible and disposable. Every developer gets the
same version, user, and database name, and `down -v` resets cleanly. The full
application compose stack (both services + DB together) is deferred to M7.

### 4. Credentials via environment variables

No credentials are hardcoded. `.env.example` documents every required variable
with placeholder values; each developer copies it to a gitignored `.env`. The
compose file and both services read their configuration from these variables.

## Consequences

**Positive**

- Realistic, portfolio-worthy stack that both services already integrate with.
- One command stands up an identical DB for any team member; no setup drift.
- Table ownership keeps the services logically decoupled on shared infra.
- Credentials never enter version control.

**Negative / deferred**

- Requires Docker on each developer's machine.
- Shared infra means a schema/migration mistake in one service can, in
  principle, affect the shared instance; ownership discipline and per-service
  migrations (#88, #95) mitigate this.
- A future extraction to per-service databases, if ever needed, is a migration,
  though an easy one given clean ownership, and the repository design pattern.

## Follow-ups

- #88 — add Spring Data JPA + Postgres driver (+ H2 for tests) to event-service
  and wire the datasource from these env vars.
- #95 — add `requirements.txt`, SQLAlchemy + psycopg to api-core, and connect it
  to the same instance.
