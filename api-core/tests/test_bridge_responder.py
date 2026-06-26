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
from pathlib import Path

import pytest

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


class TestMessageHandlerFactory:
    def test_returns_handler_for_known_type(self):
        factory = responder.MessageHandlerFactory()
        assert factory.get_handler("GET_USER_BADGES") is responder.get_user_badges
        assert factory.get_handler("GET_USER_FRIENDS") is responder.get_user_friends
        assert factory.get_handler("AWARD_BADGE") is responder.award_badge

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
        assert response["error"] == "Incorrectly formated json."

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
