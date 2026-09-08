"""Tests for ``bridge.responder`` (the Java-to-Python direction).

``responder`` is an entry-point script invoked by event-service, not a library
imported elsewhere, so it is loaded directly from its file. Three layers are
covered: the handler functions in-process (against an injected graph), the whole
script driven as a subprocess over stdin/stdout (no JVM required — only Python),
and a persistence round-trip proving a freshly-spawned process reads state that
was committed to a real database by an earlier one.
"""
from __future__ import annotations

import importlib.util
import json
import os
import subprocess
import sys
import uuid
from pathlib import Path

import pytest
import sqlalchemy

import attendance
import badges
import badges.predicates
import bootstrap
import friends
from repositories import sql

_RESPONDER_PATH = Path(__file__).resolve().parents[1] / "src" / "bridge" / "responder.py"

# Environment variables that pick the responder's backend (see the subprocess
# contract §7). ``_run`` strips them so each subprocess starts from a clean slate
# and opts into a backend explicitly.
_BACKEND_ENV_VARS = ("YSE_BRIDGE_INMEMORY", "DATABASE_URL")

# Opt a subprocess into the in-memory backend (an empty, throwaway graph that
# needs no database). Used by tests that reach a handler but assert only
# transport/envelope behaviour rather than persisted data.
_INMEMORY = {"YSE_BRIDGE_INMEMORY": "1"}


def _load_responder():
    """Imports responder.py as a standalone module without running ``main``."""
    spec = importlib.util.spec_from_file_location("bridge_responder", _RESPONDER_PATH)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


responder = _load_responder()

# The graph the in-process handler tests run against; rebuilt fresh per test by
# ``_wire_responder_to_fresh_services`` below. ``friendship_service`` is exposed
# as a convenience for the friend-graph tests.
services = bootstrap.bootstrap(register=False)
friendship_service = services.friendship_service


@pytest.fixture(autouse=True)
def _wire_responder_to_fresh_services():
    """Point the in-process responder handlers at a fresh, isolated graph.

    Importing ``responder`` has no side effects — it composes its backend lazily,
    only when the subprocess ``main`` loop first serves a real request. The
    in-process handler tests instead inject their own graph: this builds a fresh
    one through the shared ``bootstrap`` (bare in-memory repositories, evaluation
    left unregistered) and installs it on the responder module before every test,
    so seeding through ``services`` is what the handlers then serve. The
    subprocess tests spawn separate processes and are unaffected.
    """

    global services, friendship_service
    services = bootstrap.bootstrap(register=False)
    friendship_service = services.friendship_service
    responder._services = services
    yield


def _run(
    request_line: str,
    *,
    env_extra: dict[str, str] | None = None,
) -> subprocess.CompletedProcess:
    """Runs responder.py as a subprocess, feeding one request line on stdin.

    The child inherits the parent environment minus the backend-selection
    variables, so it starts from a known-clean slate and opts into a backend
    explicitly via ``env_extra`` — ``_INMEMORY`` for the in-memory graph, or
    ``{"DATABASE_URL": ...}`` for a real database. With neither (the default),
    the child has no backend configured; that is deliberate for the envelope-only
    tests, whose error paths never compose the service graph.
    """
    env = {k: v for k, v in os.environ.items() if k not in _BACKEND_ENV_VARS}
    if env_extra:
        env.update(env_extra)
    return subprocess.run(
        [sys.executable, str(_RESPONDER_PATH)],
        input=request_line,
        text=True,
        capture_output=True,
        timeout=30,
        check=False,
        env=env,
    )


def _seed_sql(url: str) -> tuple[sqlalchemy.Engine, bootstrap.Services]:
    """Builds the schema at ``url`` and returns an engine + service graph over it.

    The graph is composed of the SQLAlchemy-backed repositories (the same ones
    ``bootstrap_sql`` wires in production) so a test can seed committed rows a
    spawned responder will later read back. Evaluation is left unregistered — the
    seeding side is not where the auto-award listener belongs.
    """
    engine = sqlalchemy.create_engine(url)
    sql.metadata.create_all(engine)
    seeded = bootstrap.bootstrap(
        attendance_repository=attendance.SQLAlchemyAttendanceRepository(engine),
        friendship_repository=friends.SQLAlchemyFriendshipRepository(engine),
        badge_repository=badges.SQLAlchemyBadgeRepository(engine),
        awarded_badge_repository=badges.SQLAlchemyAwardedBadgeRepository(engine),
        register=False,
    )
    return engine, seeded


@pytest.fixture
def sql_url(tmp_path) -> str:
    """A file-backed SQLite URL shared between the test and the spawned child.

    A *file* (not ``sqlite://`` in-memory) is required: the child is a separate
    process, so it can only see rows the test committed if the database lives on
    disk.
    """
    return f"sqlite:///{tmp_path / 'bridge.db'}"


class TestHandlers:
    def test_get_user_badges_returns_awarded_badge_uuids(self):
        user = uuid.uuid4()
        badge = services.badge_service.create_badge(
            "First Event", None, badges.predicates.MinEventsAttended(threshold=1)
        )
        services.badge_service.award_badge(user, badge.get_id())

        result = responder.get_user_badges({"userId": str(user)})
        assert result == {"badges": [str(badge.get_id())]}

    def test_get_user_badges_empty_for_user_with_no_badges(self):
        result = responder.get_user_badges({"userId": str(uuid.uuid4())})
        assert result == {"badges": []}

    def test_get_user_friends_returns_accepted_friend_uuids(self):
        user, friend_one, friend_two = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        for friend in (friend_one, friend_two):
            friendship_service.send_friend_request(user, friend)
            friendship_service.accept_friend_request(user, friend)

        result = responder.get_user_friends({"userId": str(user)})
        assert set(result) == {"friends"}
        assert set(result["friends"]) == {str(friend_one), str(friend_two)}

    def test_get_user_friends_empty_for_user_with_no_friends(self):
        result = responder.get_user_friends({"userId": str(uuid.uuid4())})
        assert result == {"friends": []}

    def test_award_badge_awards_existing_badge(self):
        user = uuid.uuid4()
        badge = services.badge_service.create_badge(
            "Social5", None, badges.predicates.MinEventsAttended(threshold=1)
        )

        result = responder.award_badge(
            {"userId": str(user), "badgeId": str(badge.get_id())}
        )
        assert result == {}
        assert services.badge_service.has_badge(user, badge.get_id())

    def test_award_badge_unknown_badge_raises(self):
        with pytest.raises(ValueError):
            responder.award_badge(
                {"userId": str(uuid.uuid4()), "badgeId": str(uuid.uuid4())}
            )

    def test_record_attendance_records_and_returns_empty_payload(self):
        payload = {"userId": str(uuid.uuid4()), "eventId": str(uuid.uuid4())}
        assert responder.record_attendance(payload) == {}

    def test_record_attendance_duplicate_raises(self):
        payload = {"userId": str(uuid.uuid4()), "eventId": str(uuid.uuid4())}
        responder.record_attendance(payload)
        with pytest.raises(ValueError):
            responder.record_attendance(payload)

    def test_get_recommended_friends_recommends_a_mutual_friend(self):
        # seeker -- mutual -- candidate: the seeker and candidate share a friend
        # but are not connected, so candidate is recommended to the seeker. The
        # graph is rebuilt per test, so it is isolated from any other test's data.
        seeker, mutual, candidate = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        friendship_service.send_friend_request(seeker, mutual)
        friendship_service.accept_friend_request(seeker, mutual)
        friendship_service.send_friend_request(mutual, candidate)
        friendship_service.accept_friend_request(mutual, candidate)

        result = responder.get_recommended_friends({"userId": str(seeker)})
        assert result == {"friends": [str(candidate)]}

    def test_get_recommended_friends_empty_for_user_with_no_friends(self):
        result = responder.get_recommended_friends(
            {"userId": str(uuid.uuid4())}
        )
        assert result == {"friends": []}

    def test_get_recommended_events_ranks_friends_attendance(self, monkeypatch):
        # The user attends nothing; their one friend is signed up for `event`, so
        # it is the sole recommendation. The bridge is stubbed by conftest; re-patch
        # bridge.get_user_events so the friend has an event to recommend.
        user, friend, event = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        friendship_service.send_friend_request(user, friend)
        friendship_service.accept_friend_request(user, friend)
        monkeypatch.setattr(
            "bridge.get_user_events",
            lambda requested_id: [event] if requested_id == friend else [],
        )

        result = responder.get_recommended_events({"userId": str(user)})
        assert result == {"events": [str(event)]}

    def test_get_recommended_events_empty_for_user_with_no_friends(self):
        result = responder.get_recommended_events({"userId": str(uuid.uuid4())})
        assert result == {"events": []}


class TestMessageHandlerFactory:
    def test_returns_handler_for_known_type(self):
        factory = responder.MessageHandlerFactory()
        assert factory.get_handler("GET_USER_BADGES") is responder.get_user_badges
        assert factory.get_handler("GET_USER_FRIENDS") is responder.get_user_friends
        assert factory.get_handler("AWARD_BADGE") is responder.award_badge
        assert (
            factory.get_handler("RECORD_ATTENDANCE")
            is responder.record_attendance
        )
        assert (
            factory.get_handler("GET_RECOMMENDED_FRIENDS")
            is responder.get_recommended_friends
        )

    def test_unknown_type_raises(self):
        factory = responder.MessageHandlerFactory()
        with pytest.raises(ValueError, match="Unable to find handler"):
            factory.get_handler("NOT_A_TYPE")


class TestResponderSubprocess:
    """The script driven end-to-end over stdio.

    The envelope-only tests pass no backend: their error paths (malformed JSON,
    an unknown ``requestType``, a missing ``requestType``) are handled before the
    service graph is ever composed, so the process needs no database. Tests that
    reach a real handler opt into the empty in-memory backend (``_INMEMORY``),
    since they assert transport shape rather than persisted data.
    """

    def test_ok_response_for_empty_backend(self):
        request = json.dumps(
            {"requestType": "GET_USER_BADGES", "payload": {"userId": str(uuid.uuid4())}}
        )
        result = _run(request + "\n", env_extra=_INMEMORY)
        assert result.returncode == 0, result.stderr
        response = json.loads(result.stdout.strip())
        assert response == {"status": "ok", "payload": {"badges": []}}

    def test_unknown_request_type_yields_error(self):
        result = _run(json.dumps({"requestType": "NOPE", "payload": {}}) + "\n")
        assert result.returncode == 1
        response = json.loads(result.stdout.strip())
        assert response["status"] == "error"
        assert "Unable to find handler" in response["error"]

    def test_malformed_json_yields_error(self):
        result = _run("not json\n")
        assert result.returncode == 1
        response = json.loads(result.stdout.strip())
        assert response["status"] == "error"
        assert response["error"] == "Incorrectly formatted json."

    def test_missing_request_type_yields_error(self):
        result = _run(json.dumps({"payload": {}}) + "\n")
        assert result.returncode == 1
        response = json.loads(result.stdout.strip())
        assert response["status"] == "error"
        assert "Missing 'requestType'" in response["error"]

    def test_handler_exception_becomes_error_envelope(self):
        # A well-formed request whose handler fails (awarding a badge that does
        # not exist) must surface as an error envelope, not crash the process.
        request = json.dumps(
            {
                "requestType": "AWARD_BADGE",
                "payload": {
                    "userId": str(uuid.uuid4()),
                    "badgeId": str(uuid.uuid4()),
                },
            }
        )
        result = _run(request + "\n", env_extra=_INMEMORY)
        assert result.returncode == 1
        response = json.loads(result.stdout.strip())
        assert response["status"] == "error"

    def test_record_attendance_new_pair_succeeds(self):
        request = json.dumps(
            {
                "requestType": "RECORD_ATTENDANCE",
                "payload": {
                    "userId": str(uuid.uuid4()),
                    "eventId": str(uuid.uuid4()),
                },
            }
        )
        result = _run(request + "\n", env_extra=_INMEMORY)
        assert result.returncode == 0, result.stderr
        response = json.loads(result.stdout.strip())
        assert response == {"status": "ok", "payload": {}}

    def test_get_recommended_friends_empty_for_user_with_no_friends(self):
        request = json.dumps(
            {
                "requestType": "GET_RECOMMENDED_FRIENDS",
                "payload": {"userId": str(uuid.uuid4())},
            }
        )
        result = _run(request + "\n", env_extra=_INMEMORY)
        assert result.returncode == 0, result.stderr
        response = json.loads(result.stdout.strip())
        assert response == {"status": "ok", "payload": {"friends": []}}


class TestResponderPersistenceRoundTrip:
    """A cold-started responder reads state committed to a shared database.

    Each test seeds a real (file-backed SQLite) database, then spawns a *fresh*
    responder process pointed at that same database via ``DATABASE_URL`` and with
    no in-memory flag. This proves the per-call subprocess model is stateful
    across process boundaries — it reads committed data, not dead process memory
    (#218) — and replaces the old canned-repository fixtures.
    """

    def test_get_user_badges_reads_persisted_award(self, sql_url):
        user = uuid.uuid4()
        engine, seeded = _seed_sql(sql_url)
        badge = seeded.badge_service.create_badge(
            "First Event", None, badges.predicates.MinEventsAttended(threshold=1)
        )
        seeded.badge_service.award_badge(user, badge.get_id())
        engine.dispose()

        request = json.dumps(
            {"requestType": "GET_USER_BADGES", "payload": {"userId": str(user)}}
        )
        result = _run(request + "\n", env_extra={"DATABASE_URL": sql_url})

        assert result.returncode == 0, result.stderr
        response = json.loads(result.stdout.strip())
        assert response == {
            "status": "ok",
            "payload": {"badges": [str(badge.get_id())]},
        }

    def test_get_user_friends_reads_persisted_friend_graph(self, sql_url):
        user, friend_one, friend_two = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        engine, seeded = _seed_sql(sql_url)
        for friend in (friend_one, friend_two):
            seeded.friendship_service.send_friend_request(user, friend)
            seeded.friendship_service.accept_friend_request(user, friend)
        engine.dispose()

        request = json.dumps(
            {"requestType": "GET_USER_FRIENDS", "payload": {"userId": str(user)}}
        )
        result = _run(request + "\n", env_extra={"DATABASE_URL": sql_url})

        assert result.returncode == 0, result.stderr
        response = json.loads(result.stdout.strip())
        assert response["status"] == "ok"
        assert set(response["payload"]["friends"]) == {
            str(friend_one),
            str(friend_two),
        }

    def test_get_recommended_friends_reads_persisted_graph(self, sql_url):
        # seeker -- mutual -- candidate, persisted to the database; the spawned
        # responder must recommend the candidate off the stored friend graph.
        seeker, mutual, candidate = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        engine, seeded = _seed_sql(sql_url)
        seeded.friendship_service.send_friend_request(seeker, mutual)
        seeded.friendship_service.accept_friend_request(seeker, mutual)
        seeded.friendship_service.send_friend_request(mutual, candidate)
        seeded.friendship_service.accept_friend_request(mutual, candidate)
        engine.dispose()

        request = json.dumps(
            {
                "requestType": "GET_RECOMMENDED_FRIENDS",
                "payload": {"userId": str(seeker)},
            }
        )
        result = _run(request + "\n", env_extra={"DATABASE_URL": sql_url})

        assert result.returncode == 0, result.stderr
        response = json.loads(result.stdout.strip())
        assert response == {
            "status": "ok",
            "payload": {"friends": [str(candidate)]},
        }

    def test_record_attendance_duplicate_of_persisted_record_errors(self, sql_url):
        # An attendance committed by one process must be seen by the next: the
        # spawned responder re-recording the same pair is rejected as a duplicate.
        user, event = uuid.uuid4(), uuid.uuid4()
        engine, seeded = _seed_sql(sql_url)
        seeded.attendance_service.record_attendance(user, event)
        engine.dispose()

        request = json.dumps(
            {
                "requestType": "RECORD_ATTENDANCE",
                "payload": {"userId": str(user), "eventId": str(event)},
            }
        )
        result = _run(request + "\n", env_extra={"DATABASE_URL": sql_url})

        assert result.returncode == 1
        response = json.loads(result.stdout.strip())
        assert response["status"] == "error"
        assert "already been recorded" in response["error"]
