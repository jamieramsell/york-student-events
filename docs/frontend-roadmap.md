# Frontend Roadmap (M8 onwards)

This document is the forward-looking plan for the web frontend and the features that follow it. It is the counterpart to `CHANGELOG.md`: milestones are described here *before* they are built, and enter the changelog only when delivered. Expect this document to be revised as milestones close.

**Prerequisite:** the backend reaches v1.0.0 via M5–M7 — including M6, which delivers the live event-service HTTP endpoints (with the routes for api-core features delegating over the subprocess bridge, so event-service remains the single HTTP gateway).

## Approach

Two principles shape the milestone boundaries:

1. **Design the interface before wiring data.** The full UI is built first against typed mock fixtures, and the live API is swapped in afterwards. The fixture types mirror `docs/api-spec.yaml`, so the swap is a change of data source, not a rewrite.
2. **The ordering doubles as a learning curve.** Each milestone introduces one new layer of frontend skills, in the same build-on-itself fashion as M0–M7: static components → local state → async data fetching → interactive mutation flows.

**Stack:** React + Vite + TypeScript, in a new top-level `frontend/` directory alongside `api-core/` and `event-service/`.

**Versioning:** frontend milestones are backwards-compatible additions to the platform, so each is a MINOR bump on top of v1.0.0. The first breaking architectural shift (real-time chat) is reserved for v2.0.0.

## Scheduled milestones

### M8 — Frontend Foundation & Design System (target: v1.1.0)

Scaffolding and visual language for the web client — zero state, zero network. The mirror of M1, but for the frontend.

- Scaffold `frontend/` with Vite + React + TypeScript; ESLint + Prettier
- Frontend CI workflow alongside `build.yml` / `lint.yml`
- Routing skeleton (react-router) with a static placeholder page per planned screen
- Shared layout: navigation, page shell
- Design-system primitives: colour/spacing tokens, Button, Card, form inputs
- Newcomer starter issues: small, self-contained components

*Skills introduced: JSX, components, props, CSS, project tooling.*

### M9 — Static Screens with Mock Data (target: v1.2.0)

The "design the interface" milestone: every core screen exists and is navigable, rendering typed fixture data. Still no network code.

- TypeScript interfaces mirroring the `docs/api-spec.yaml` resources
- Typed mock fixtures for events, users, venues, cohorts, friendships, badges
- Event browsing and detail views; registration/login forms (non-functional)
- Profile pages, cohort views, friends list, recommendations feed, badge display

*Skills introduced: `useState`, list rendering, forms, conditional rendering, TypeScript interfaces.*

### M10 — API Integration: event-service (target: v1.3.0)

Swap fixtures for live calls behind the same TypeScript interfaces. Depends on the M6 backend endpoints being live.

- API client layer (base URL handling, typed request/response helpers)
- CORS configuration on the Java side
- Live data for events, users, venues, cohorts, and subscriptions
- Loading and error states throughout
- Working registration and login

*Skills introduced: `useEffect`, fetch, async, loading/error handling.*

### M11 — Social & Gamification UI (target: v1.4.0)

Purely frontend — the endpoints already exist from M6 (event-service routes delegating to api-core over the subprocess bridge). The step up from M10 is interactivity: mutations and multi-step flows rather than read-and-render.

- Friend-request flows: send / accept / decline, with their state transitions
- Recommendations feed backed by live matching data
- Badge display backed by live award data

*Skills introduced: mutations, optimistic/refetch patterns, multi-step interactive flows.*

## Later milestones (order to be decided)

These are named by theme only and get an `Mx` number when scheduled, since their order is flexible. Versions beyond v2.0.0 are likewise assigned at scheduling time.

| Theme | Semver impact | Notes |
|---|---|---|
| **Real-time Chat & Moderation** | MAJOR (v2.0.0 if scheduled first) | Message filtering and report/moderation tooling; first milestone needing new infrastructure (WebSockets or similar, moderation data model). The heaviest item on this list. |
| **Friend Network Maps** | MINOR | Interactive friend-graph visualisation; may need a nodes/edges graph endpoint (delegating over the bridge, per the M6 pattern). |
| **Push Notifications** | MINOR | Extends the Observer pattern from in-process notification to real delivery (web push and/or email) with user notification preferences. |
| **Production Hardening & Deployment** | MINOR | Hosting, monitoring/logging, performance work, full CI/CD. Scheduled last. |
