# Subprocess JSON Communication Contract

This document defines the JSON contract for the subprocess bridge between the Python `api-core` and Java `event-service` backends. It is the single source of truth for the wire format; either side may be reimplemented against this document alone.

## 1. Overview

The bridge is a **JSON-over-stdio** contract. There is no long-running server: for each call, one service spawns the other as a **fresh child process**, writes a single request, reads a single response, and the child exits.

The bridge is **bidirectional** — each service both *issues* requests and *answers* them, and each direction has its own client and responder:

| Direction | Client (spawns) | Responder (answers) |
|---|---|---|
| api-core → event-service | `api-core/src/bridge/client.py` | `york.studentevents.subprocess.SubprocessResponder` |
| event-service → api-core | `SubprocessRequestFactory` | `api-core/src/bridge/responder.py` |

Which responder answers a given request is fixed by **data ownership**: each `requestType` is served by exactly one side (see §4). A request sent to the side that does not own it is rejected with an error envelope.

## 2. Framing and lifecycle

- **Encoding:** UTF-8.
- **Framing:** newline-delimited JSON - each envelope is a single JSON object on one line, terminated by `\n`. Responders parse per line and callers read the response line back.
- **Lifecycle:** one request per spawned process. The client writes its request to the child's stdin (then closes it) and reads the response from the child's stdout.
- **Exit status is part of the contract.** A successful (`ok`) response is paired with exit code `0`; an `error` response is paired with a **non-zero** exit code. Callers rely on the exit code to distinguish success from failure and should treat a non-zero exit as an error even if stdout is empty or unparseable.

## 3. Request envelope

Every request is a JSON object with two fields:

- `requestType` — a string naming the operation (see §4).
- `payload` — an object carrying that operation's arguments.

```json
{
  "requestType": "GET_USER_BADGES",
  "payload": {
    "userId": "86aa54b8-2d08-498b-aee9-b2c26a97717e"
  }
}
```

All IDs (users, badges, events) are transmitted as their canonical UUID string form.

## 4. Request types

Each row lists the responder that owns the type, the request payload fields, and the fields of the success (`ok`) response payload.

| `requestType` | Owned by | Request payload | Response payload |
|---|---|---|---|
| `GET_USER_BADGES` | api-core | `userId` (UUID) | `badges`: array of badge UUIDs |
| `GET_USER_FRIENDS` | api-core | `userId` (UUID) | `friends`: array of user UUIDs |
| `AWARD_BADGE` | api-core | `userId` (UUID), `badgeId` (UUID) | *(empty — success signalled by the `ok` status)* |
| `GET_RECOMMENDED_EVENTS` | api-core | `userId` (UUID) | `events`: array of event UUIDs |
| `GET_USER_EVENTS` | event-service | `userId` (UUID) | `events`: array of event UUIDs |
| `RECORD_ATTENDANCE` | api-core | `userId` (UUID), `eventId` (UUID) | *(empty — success signalled by the `ok` status)* |
| `GET_RECOMMENDED_FRIENDS` | api-core | `userId` (UUID) | `friends`: array of user UUIDs |
| `GET_EVENT_INFO` | event-service | `eventId` (UUID) | `host` (UUID), `start` (str, ISO-8601 formatted datetime), `category` (str) |
| `GET_BATCH_EVENT_INFO` | event-service | `eventIds` (array of event UUIDs) | `events`: dict keyed by event IDs, values store event info (see payload returned by `GET_EVENT_INFO`)|
| `BADGE_AWARDED` | event-service | `userId` (UUID), `badgeName` (string) | *(empty — success signalled by the `ok` status)* |

> **Why send `badgeName` and not `badgeId` on `BADGE_AWARDED`?** `BADGE_AWARDED` is a fire-and-forget notification that lets event-service tell a student they earned a badge. Badges are owned by `api-core`, so event-service cannot resolve a badge UUID to anything displayable on its own; sending the human-readable name keeps the notification self-contained. One request is sent per newly awarded badge.

## 5. Response envelope

A successful response has `status: "ok"` and a `payload` object whose shape is determined by the request type (see §4).

```json
{
  "status": "ok",
  "payload": {
    "badges": [
      "3f6c1a2e-9b7d-4c85-8f21-0a1b2c3d4e5f",
      "7a2b9c4d-1e6f-4a83-b5c2-9d8e7f6a5b4c"
    ]
  }
}
```

## 6. Error envelope

If processing fails, the response has `status: "error"` and an `error` field holding a human-readable message, and the process exits non-zero.

```json
{
  "status": "error",
  "error": "User 86aa54b8-2d08-498b-aee9-b2c26a97717e not found"
}
```

Errors are raised for a malformed or non-JSON request, a missing `requestType` or `payload`, an unknown request type, a request type not owned by the receiving side, a missing or malformed payload field (e.g. an invalid `userId`), or an unknown entity. The `error` message is intended for humans and diagnostics; it is **not** a stable, machine-parsable part of the contract — callers should branch on `status` and the exit code, not on the message text.

## 7. Responder backend selection (api-core)

The api-core responder (`api-core/src/bridge/responder.py`) holds no state of its own. The event-service spawns a fresh process per call (see §2), meaning that anything kept in memory dies when that process exits. State is therefore external, with the responder composing its service graph over the shared database, so that a badge awarded or an attendance recorded by one call is committed to the database and read back by the next freshly-spawned process. This is what makes the subprocess-per-call model stateful; the rationale is recorded in [ADR-0001](adr/0001-inter-service-transport.md).

Which backend the responder composes is chosen from the environment:

| Variable | Required? | Effect |
|---|---|---|
| `DATABASE_URL` | **Yes**, unless `YSE_BRIDGE_INMEMORY` is set | Connection URL for the shared database. The responder persists to and reads from it (via `bootstrap_sql()`). If it is unset, the responder **fails fast at start-up** rather than serving an empty, non-persistent graph. |
| `YSE_BRIDGE_INMEMORY` | No, **test only** | When set to any non-empty value, the responder composes a throwaway in-memory graph and does not require `DATABASE_URL`. Must not be set in production. |

- **Default (production):** `DATABASE_URL` must be present. A misconfigured bridge is thus a loud start-up error, not silent data loss.
- **`YSE_BRIDGE_INMEMORY` (tests only):** exists solely so the responder process can start *without* a database in test environments, for example within the Java→Python integration tests, which spawn the real responder to exercise the wire contract but only assert envelope shapes (`ok` / empty payload / `error`) rather than persisted data. Each spawned process starts with an empty in-memory graph which dies with the process, so this mode carries no data between calls and offers no persistence guarantees. It is therefore never a production configuration.
