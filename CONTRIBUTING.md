# Contributing to york-student-events

Welcome! This guide takes a new contributor from a fresh checkout to a green test run and a first pull request. It focuses on **local setup, running the services, and where to start**. For the milestone-based branching model and release process, see the [Contributing section of the README](README.md#contributing).

---

## 1. Prerequisites

- **Python 3.12+** — the repo pins **3.13.14** (see [`.python-version`](.python-version)); [pyenv](https://github.com/pyenv/pyenv) picks this up automatically. Confirm `python --version` resolves to 3.12+ (on some systems the pyenv-managed interpreter is `python`, while `python3` is an older system build).
- **Java 21+ (JDK)** — needed to build/run `event-service` *and* to run the Python↔Java bridge integration tests. Confirm with `java --version`.
- **No third-party Python packages yet.** The `api-core` suite is standard-library only, so there is no `pip install` step. (This changes in **M5**, when the persistence layer introduces a `requirements.txt`.)

---

## 2. api-core (Python)

Run the suite **from the repo root** — `pytest.ini` sets `pythonpath` and `testpaths`, so pytest must be launched from the root, not from `api-core/`:

```bash
python -m pytest api-core/tests/
```

Some tests are marked `integration`: they spawn the real Java `event-service` over the subprocess bridge and require it to be compiled first (see §4). To skip them and run only the fast, self-contained tests:

```bash
python -m pytest api-core/tests/ -m "not integration"
```

---

## 3. event-service (Java / Maven)

Use the bundled Maven wrapper (`./mvnw`) — never a system `mvn` — from the `event-service/` directory:

```bash
cd event-service
./mvnw spring-boot:run     # run the service
./mvnw test                # unit tests
./mvnw verify              # compile + test + Checkstyle (Google Java Style)
```

> ⚠️ **CI runs Checkstyle.** `./mvnw test` on its own will *not* catch style violations, but CI (and `./mvnw verify`) will. Run `./mvnw verify` before you push so your PR doesn't fail on formatting.

---

## 4. Running both services together (the subprocess bridge)

The two services communicate over a **JSON-over-stdio subprocess bridge**. There is no long-running server, instead each call spawns the other side as a fresh child process. The wire contract is the single source of truth in [`docs/subprocess-contract.md`](docs/subprocess-contract.md).

The api-core integration tests drive the *real* Java responder, so `event-service` must be **compiled** first:

```bash
cd event-service && ./mvnw compile   # produces target/classes and target/cp.txt
cd .. && python -m pytest api-core/tests/   # integration tests now runnable
```

If you see `SubprocessError: event-service is not built`, you skipped the compile step above.

---

## 5. Getting oriented (a ~10-minute read)

- **[`CLAUDE.md`](CLAUDE.md)** — the fullest architecture + conventions overview: the two-service split, per-language code style, and the four core patterns (repository, observer, predicate DSL, subprocess bridge).
- **[`README.md`](README.md)** — project overview, structure tree, and branching model.
- **[`docs/subprocess-contract.md`](docs/subprocess-contract.md)** — cross-service wire format.
- **[`docs/api-spec.yaml`](docs/api-spec.yaml)** — the *intended* REST contract (not live yet).

The one pattern worth internalising first is the **repository pattern**, because **M5 re-implements it against a database**. Read one in-memory example on each side and you'll recognise the shape everywhere:

- Python — [`api-core/src/badges/badge_repository.py`](api-core/src/badges/badge_repository.py)
- Java — [`event-service/.../repository/inmemory/InMemoryEventRepository.java`](event-service/src/main/java/york/studentevents/repository/inmemory/InMemoryEventRepository.java)

Every persistent repository added in M5 follows that same interface.

---

## 6. Workflow (quick version)

Full details live in the [README Contributing section](README.md#contributing). In short:

1. **Open or claim an issue first** — start with issues labelled [`good first issue`](https://github.com/jamieramsell/york-student-events/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22).
2. **Branch** using the milestone convention `<milestone>/<label>/<name>` (e.g. `m5/feat/persistent-badge-repository`), branched from the milestone's `stable-<milestone>` branch.
3. **Commit** with [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`, …; breaking changes append `!`).
4. **Open a PR** with the [template](.github/pull_request_template.md); keep it small (one entity/store per PR is ideal) and make sure the build, test, and lint checks pass.

The **`blocked`** label means an issue is waiting on an upstream one. It is managed automatically, so a blocked issue opens up once its dependencies land. You don't need to remove the label by hand.

---

## 7. Where to start

New to the codebase? Filter the tracker by the [`good first issue`](https://github.com/jamieramsell/york-student-events/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22) label. These are scoped to be approachable and each mirrors a worked reference implementation you can pattern-match against.