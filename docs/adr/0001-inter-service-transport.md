# ADR-0001: Inter-service transport between api-core and event-service

**Status:** Proposed
**Date:** 2026-07-20
**Deciders:** (owner)

## Context

api-core (Python) and event-service (Java/Spring Boot) communicate over a
**subprocess-per-call JSON-over-stdio bridge** (`api-core/src/bridge/`,
`event-service/src/main/java/york/studentevents/subprocess/`, contract in
`docs/subprocess-contract.md`): each call spawns a fresh process of the other
side, exchanges one request/response envelope, and exits. Both sides' handlers
are currently canned stubs.

Wiring the badge auto-award feature (record attendance → evaluate badges → notify
event-service) exposed repeated friction with this model, and raised the
question: should we keep the bridge or move to long-lived HTTP services? Crucially,
this must be decided **without conflating it with the separate persistence
question** (milestone M5).

## Decision drivers — two orthogonal axes

1. **State / persistence** — do badge definitions, awards, and events survive
   across calls and dedupe correctly?
2. **Transport** — how the two services talk (process-per-call vs long-lived
   endpoints): cold-start cost, recursive spawning, fragility, concurrency.

Conflating these is the trap: some of the current pain is (1), some is (2).

## What persistence (M5) fixes on its own — independent of transport

A **shared database** makes even the subprocess model stateful: a fresh
`responder.py` per call reads accumulated badges/awards from the DB rather than
from dead process memory. This resolves badge definitions not surviving, awards
not accumulating or deduping, and empty repositories per call — i.e. most of the
current workarounds (`register=False`, canned repositories, per-call reseeding
concerns). **The bulk of the auto-award blocker is a persistence problem, not a
transport problem.**

## What persistence does NOT fix — intrinsic to subprocess-per-call

- Cold-start cost per call (a JVM per request; a DB connection per fresh process
  makes it worse).
- Recursive process spawning (`RECORD_ATTENDANCE` spawns Python → evaluation
  spawns Java for `GET_EVENT_INFO` → …).
- Transport fragility: classpath resolution, "is event-service built", 30s
  timeouts, parse-last-stdout-line, one-request-per-process, no concurrency /
  streaming / real status codes.
- No throughput / scaling story.

## Options considered

- **A — Keep the bridge; add shared persistence (M5).** Fixes the state family;
  leaves the transport costs. Lowest effort; matches the current architecture.
- **B — Migrate to long-lived HTTP services (+ persistence).** event-service
  exposes real Spring endpoints (per `docs/api-spec.yaml`); api-core gains a web
  framework (FastAPI/Flask — a break from the "no third-party dependencies"
  convention) and an HTTP client. Fixes both axes; largest effort plus new
  operational surface. Note HTTP still is not durable persistence — a long-lived
  process only gives in-run state; surviving restarts still needs a database.
- **C — Decouple the axes.** Do persistence (M5) now; treat HTTP as a separate,
  trigger-driven transport decision revisited when a concrete cost appears.

## Decision (recommended): Option C

Persistence and transport are separate decisions and should not be bundled.
Near-term, **M5 persistence** removes most of the friction encountered and
unblocks auto-award — keep the subprocess bridge for now. Adopt **HTTP** only when
a concrete trigger appears, e.g.:

- per-call cold-start latency becomes user-visible or load-bearing,
- the recursive-spawn chain (attendance → evaluation → event-info) becomes hot,
- throughput / concurrency needs exceed process-per-request,
- or we want the OpenAPI surface (`docs/api-spec.yaml`) live for a real frontend.

For a student-events app at modest scale and relaxed latency, bridge + shared DB
may be sufficient for a long time; HTTP is the right *eventual* target, not a
prerequisite for anything M5 already delivers.

## Consequences

**Positive**

- Avoids a premature, large HTTP rewrite.
- Focuses effort on M5, which is the actual unblocker.
- Keeps the HTTP option open with explicit triggers.
- Preserves the "no third-party dependencies" convention until there is a real
  reason to break it.

**Negative / deferred**

- The transport warts persist until a trigger fires.
- When HTTP is eventually adopted it will be a larger migration than if done now.
- The auto-award roadmap's best-effort-evaluation fix is still needed regardless,
  since a synchronous cross-service call can fail on any transport.

## Follow-ups

- Land the badge-evaluation **best-effort** fix (so a failed cross-service
  `get_event_info` can never roll back a real attendance) independent of this
  decision.
- When M5 is scoped, note in its issue that it is expected to remove the
  `register=False` / canned-repository workarounds.
- Create a tracking issue to re-evaluate HTTP against the triggers above.
