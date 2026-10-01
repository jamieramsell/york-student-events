"""Full round-trip integration test: the Python client spawns the real Java responder.

This is skipped unless ``java`` is on PATH and the event-service build output
(``target/classes`` + ``target/cp.txt``) exists; the fixture attempts a build
first and skips with a clear message if the toolchain is unavailable.

Each request spawns the responder as a fresh JVM under ``YSE_BRIDGE_INMEMORY``, so
it composes a throwaway in-memory graph and needs no database. That graph starts
*empty* and dies with the process, and a cross-JVM child cannot see data seeded in
this test's process, so these tests assert only envelope *shapes* (the ok
round-trip and the not-found / error paths). The real-data assertions (that a
seeded event yields its actual host, start and category) live in event-service's
``SubprocessResponderInProcessTest``, which drives the handlers in-process over a
seeded database.
"""
from __future__ import annotations

import shutil
import subprocess
import uuid
from pathlib import Path

import pytest

from bridge import client
from bridge.client import (
    SubprocessError,
    get_batch_event_info,
    get_event_info,
    get_user_events,
    notify_badge_awarded,
)

# Spawns the Java responder, so it shares the suite-wide `integration` marker
# (registered in pytest.ini) and can be deselected with `-m 'not integration'`.
pytestmark = pytest.mark.integration

_EVENT_SERVICE = Path(__file__).resolve().parents[2] / "event-service"

# Arbitrary well-formed UUIDs; unknown in the empty in-memory graph.
SOME_USER_ID = "11111111-1111-1111-1111-111111111111"
SOME_EVENT_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
SECOND_EVENT_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"


@pytest.fixture(scope="module", autouse=True)
def _event_service_built():
    """Ensures event-service is built, or skips the module."""
    if shutil.which("java") is None:
        pytest.skip("java is not on PATH")

    if not client._CLASSES_DIR.is_dir() or not client._CLASSPATH_FILE.is_file():
        mvnw = _EVENT_SERVICE / "mvnw"
        if not mvnw.exists():
            pytest.skip("event-service is not built and mvnw is missing")
        result = subprocess.run(
            [str(mvnw), "-q", "compile"],
            cwd=_EVENT_SERVICE,
            capture_output=True,
            text=True,
            check=False,
        )
        if (
            result.returncode != 0
            or not client._CLASSES_DIR.is_dir()
            or not client._CLASSPATH_FILE.is_file()
        ):
            pytest.skip(
                "event-service build output unavailable (is the dependency:build-classpath "
                f"goal wired into the build?):\n{result.stdout}\n{result.stderr}"
            )


@pytest.fixture(autouse=True)
def _inmemory_bridge(monkeypatch):
    monkeypatch.setenv("YSE_BRIDGE_INMEMORY", "1")
    

class TestRoundTrip:
    # ok-shape: a request needing no data completes a full round trip //

    def test_batch_empty_list_returns_empty(self):
        assert get_batch_event_info([]) == {}

    # not-found: every id is unknown in the empty graph //

    def test_unknown_user_raises_not_recognised(self):
        with pytest.raises(SubprocessError, match="not recognised"):
            get_user_events(SOME_USER_ID)

    def test_unknown_event_raises_not_recognised(self):
        with pytest.raises(SubprocessError, match="not recognised"):
            get_event_info(SOME_EVENT_ID)

    def test_batch_with_any_unknown_event_raises_not_recognised(self):
        with pytest.raises(SubprocessError, match="not recognised"):
            get_batch_event_info(
                [uuid.UUID(SOME_EVENT_ID), uuid.UUID(SECOND_EVENT_ID)]
            )

    def test_badge_awarded_notification_unknown_user_raises_not_recognised(self):
        with pytest.raises(SubprocessError, match="not recognised"):
            notify_badge_awarded(SOME_USER_ID, "First Event")
