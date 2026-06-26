"""Full round-trip integration test: the Python client spawns the real Java responder.

This is skipped unless ``java`` is on PATH and the event-service build output
(``target/classes`` + ``target/cp.txt``) exists; the fixture attempts a build
first and skips with a clear message if the toolchain is unavailable. The canned
constants below MUST match those in ``SubprocessResponder.java``.
"""
from __future__ import annotations

import shutil
import subprocess
from pathlib import Path

import pytest

from bridge import client
from bridge.client import SubprocessError, get_user_events

_EVENT_SERVICE = Path(__file__).resolve().parents[2] / "event-service"

KNOWN_USER_ID = "11111111-1111-1111-1111-111111111111"
EXPECTED_EVENTS = [
    "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
]


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
