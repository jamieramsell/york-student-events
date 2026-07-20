"""Full round-trip integration test: the Python client spawns the real Java responder.

This is skipped unless ``java`` is on PATH and the event-service build output
(``target/classes`` + ``target/cp.txt``) exists; the fixture attempts a build
first and skips with a clear message if the toolchain is unavailable. The canned
constants below MUST match those in ``SubprocessResponder.java``.
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

KNOWN_USER_ID = "11111111-1111-1111-1111-111111111111"
# get_user_events parses the responder's event IDs into UUIDs, so the expected
# canned events are UUID objects (not the raw strings the responder emits).
EXPECTED_EVENTS = [
    uuid.UUID("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
    uuid.UUID("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
]

# One of the canned events known to the responder, with its expected info payload.
KNOWN_EVENT_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
EXPECTED_EVENT_INFO = {
    "host": "22222222-2222-2222-2222-222222222222",
    "start": "2026-09-15T18:00:00",
    "category": "SOCIAL",
}

# The responder's second canned event, used to exercise batch fetches.
SECOND_EVENT_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
SECOND_EVENT_INFO = {
    "host": "33333333-3333-3333-3333-333333333333",
    "start": "2026-10-01T14:30:00",
    "category": "ACADEMIC",
}


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


class TestRoundTrip:
    def test_known_user_returns_canned_events(self):
        assert get_user_events(KNOWN_USER_ID) == EXPECTED_EVENTS

    def test_unknown_user_raises_not_found(self):
        with pytest.raises(SubprocessError, match="not found"):
            get_user_events("00000000-0000-0000-0000-000000000000")

    def test_known_event_returns_canned_info(self):
        assert get_event_info(KNOWN_EVENT_ID) == EXPECTED_EVENT_INFO

    def test_unknown_event_raises_not_found(self):
        with pytest.raises(SubprocessError, match="not found"):
            get_event_info("99999999-9999-9999-9999-999999999999")

    def test_batch_returns_canned_info_in_request_order(self):
        result = get_batch_event_info(
            [uuid.UUID(SECOND_EVENT_ID), uuid.UUID(KNOWN_EVENT_ID)]
        )
        # Order follows the request, not the responder's internal map ordering.
        assert result == [SECOND_EVENT_INFO, EXPECTED_EVENT_INFO]

    def test_batch_single_event_matches_get_event_info(self):
        [info] = get_batch_event_info([uuid.UUID(KNOWN_EVENT_ID)])
        assert info == EXPECTED_EVENT_INFO == get_event_info(KNOWN_EVENT_ID)

    def test_batch_empty_list_returns_empty(self):
        assert get_batch_event_info([]) == []

    def test_batch_with_any_unknown_event_raises_not_found(self):
        with pytest.raises(SubprocessError, match="not found"):
            get_batch_event_info(
                [
                    uuid.UUID(KNOWN_EVENT_ID),
                    uuid.UUID("99999999-9999-9999-9999-999999999999"),
                ]
            )

    def test_badge_awarded_notification_for_known_user_succeeds(self):
        # Fire-and-forget: a successful notification returns None (empty payload).
        assert notify_badge_awarded(KNOWN_USER_ID, "First Event") is None

    def test_badge_awarded_notification_unknown_user_raises(self):
        with pytest.raises(SubprocessError, match="not found"):
            notify_badge_awarded(
                "00000000-0000-0000-0000-000000000000", "First Event"
            )
