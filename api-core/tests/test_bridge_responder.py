"""Tests for ``bridge.responder`` (the Java-to-Python direction).

``responder`` is an entry-point script invoked by event-service, not a library
imported elsewhere, so it is loaded directly from its file. Two layers are
covered: the handler functions in-process, and the whole script driven as a
subprocess over stdin/stdout (no JVM required — only Python).
"""
from __future__ import annotations

import importlib.util
import json
import subprocess
import sys
import uuid
from pathlib import Path

import pytest

import friends
from attendance.attendance_repository import CANNED_ATTENDEE_ID, CANNED_EVENT_ID
from friends.friendship_repository import (
    CANNED_FRIEND_SEEKER_ID,
    CANNED_RECOMMENDED_FRIEND_ID,
)

_RESPONDER_PATH = Path(__file__).resolve().parents[1] / "src" / "bridge" / "responder.py"


def _load_responder():
    """Imports responder.py as a standalone module without running ``main``."""
    spec = importlib.util.spec_from_file_location("bridge_responder", _RESPONDER_PATH)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


responder = _load_responder()


def _run(request_line: str) -> subprocess.CompletedProcess:
    """Runs responder.py as a subprocess, feeding one request line on stdin."""
    return subprocess.run(
        [sys.executable, str(_RESPONDER_PATH)],
        input=request_line,
        text=True,
        capture_output=True,
        timeout=30,
    )


class TestHandlers:
    def test_get_user_badges_returns_stub(self):
        assert responder.get_user_badges({}) == {"badges": ["First Event", "Social5"]}

    def test_get_user_friends_returns_stub(self):
        assert responder.get_user_friends({}) == {"friends": ["James", "Jamie"]}

    def test_award_badge_raises(self):
        with pytest.raises(ValueError):
            responder.award_badge({})

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
        # friends repository is reset per test (conftest), so this graph is
        # isolated from the responder's canned data.
        seeker, mutual, candidate = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        friends.send_friend_request(seeker, mutual)
        friends.accept_friend_request(seeker, mutual)
        friends.send_friend_request(mutual, candidate)
        friends.accept_friend_request(mutual, candidate)

        result = responder.get_recommended_friends({"userId": str(seeker)})
        assert result == {"friends": [str(candidate)]}

    def test_get_recommended_friends_empty_for_user_with_no_friends(self):
        result = responder.get_recommended_friends(
            {"userId": str(uuid.uuid4())}
        )
        assert result == {"friends": []}


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
    def test_ok_response(self):
        request = json.dumps({"requestType": "GET_USER_BADGES", "payload": {"userId": 1}})
        result = _run(request + "\n")
        assert result.returncode == 0
        response = json.loads(result.stdout.strip())
        assert response == {"status": "ok", "payload": {"badges": ["First Event", "Social5"]}}

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
        result = _run(json.dumps({"requestType": "AWARD_BADGE", "payload": {}}) + "\n")
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
        result = _run(request + "\n")
        assert result.returncode == 0
        response = json.loads(result.stdout.strip())
        assert response == {"status": "ok", "payload": {}}

    def test_record_attendance_duplicate_of_canned_record_errors(self):
        # The subprocess serves the canned repository, which is pre-seeded with
        # this pair, so re-recording it must be rejected as a duplicate.
        request = json.dumps(
            {
                "requestType": "RECORD_ATTENDANCE",
                "payload": {
                    "userId": str(CANNED_ATTENDEE_ID),
                    "eventId": str(CANNED_EVENT_ID),
                },
            }
        )
        result = _run(request + "\n")
        assert result.returncode == 1
        response = json.loads(result.stdout.strip())
        assert response["status"] == "error"
        assert "already been recorded" in response["error"]

    def test_get_recommended_friends_returns_canned_recommendation(self):
        # The subprocess serves the canned friend graph, in which the seeker and
        # the recommended user share a mutual friend but are not connected, so
        # the recommendation is deterministic.
        request = json.dumps(
            {
                "requestType": "GET_RECOMMENDED_FRIENDS",
                "payload": {"userId": str(CANNED_FRIEND_SEEKER_ID)},
            }
        )
        result = _run(request + "\n")
        assert result.returncode == 0
        response = json.loads(result.stdout.strip())
        assert response == {
            "status": "ok",
            "payload": {"friends": [str(CANNED_RECOMMENDED_FRIEND_ID)]},
        }

    def test_get_recommended_friends_empty_for_user_with_no_friends(self):
        request = json.dumps(
            {
                "requestType": "GET_RECOMMENDED_FRIENDS",
                "payload": {"userId": str(uuid.uuid4())},
            }
        )
        result = _run(request + "\n")
        assert result.returncode == 0
        response = json.loads(result.stdout.strip())
        assert response == {"status": "ok", "payload": {"friends": []}}
