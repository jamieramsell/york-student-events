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
| `AWARD_BADGE` | api-core | `userId` (UUID), `badgeName` (string) | *(empty — success signalled by the `ok` status)* |
| `GET_RECOMMENDED_EVENTS` | api-core | `userId` (UUID) | `events`: array of event UUIDs |
| `GET_USER_EVENTS` | event-service | `userId` (UUID) | `events`: array of event UUIDs |
| `RECORD_ATTENDANCE` | api-core | `userId` (UUID), `eventId` (UUID) | *(empty — success signalled by the `ok` status)* |
| `GET_RECOMMENDED_FRIENDS` | api-core | `userId` (UUID) | `friends`: array of user UUIDs |
| `GET_EVENT_INFO` | event-service | `eventId` (UUID) | `host` (UUID), `start` (str, ISO-8601 formatted datetime), `category` (str) |
| `GET_BATCH_EVENT_INFO` | event-service | `eventIds` (array of event UUIDs) | `events` (array of event info- see payload returned by `GET_EVENT_INFO`)|
| `BADGE_AWARDED` | event-service | `userId` (UUID), `badgeName` (string) | *(empty — success signalled by the `ok` status)* |

> **Why `badgeName` and not `badgeId`?** `BADGE_AWARDED` is a fire-and-forget notification that lets event-service tell a student they earned a badge. Badges are owned by `api-core`, so event-service cannot resolve a badge UUID to anything displayable on its own; sending the human-readable name keeps the notification self-contained and mirrors the existing `AWARD_BADGE` type (the reverse direction), which is also keyed by `badgeName`. One request is sent per newly awarded badge.

> **Note:** the responder handlers are still stubs — they return canned data rather than querying real repositories, and `GET_USER_BADGES` currently emits placeholder badge *names* instead of UUIDs. The schema above describes the intended contract; wiring the handlers up to it is tracked separately.

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
