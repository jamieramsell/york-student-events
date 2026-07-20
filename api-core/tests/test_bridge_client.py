"""Unit tests for ``bridge.client``.

These exercise the client's logic in isolation: the Java responder is replaced
with a mocked ``subprocess.Popen``, so no JDK or event-service build is needed.
The full Python-to-Java round trip is covered separately in
``test_bridge_integration.py``.
"""
from __future__ import annotations

import json
import os
import subprocess
import uuid
from unittest import mock

import pytest

from bridge import client
from bridge.client import (
    SubprocessError,
    get_batch_event_info,
    get_event_info,
    get_user_events,
    notify_badge_awarded,
)

OK_LINE = json.dumps({"status": "ok", "payload": {"events": ["e1", "e2"]}}) + "\n"


def _fake_process(stdout: str = "", stderr: str = "", returncode: int = 0):
    """Builds a stand-in for the object returned by ``subprocess.Popen``."""
    proc = mock.MagicMock()
    proc.communicate.return_value = (stdout, stderr)
    proc.returncode = returncode
    return proc


def _patch_popen(proc):
    """Patches the Popen the client uses to return ``proc``."""
    return mock.patch("bridge.client.subprocess.Popen", return_value=proc)


class TestCallResponder:
    def test_ok_response_returns_payload(self):
        proc = _fake_process(stdout=OK_LINE)
        with _patch_popen(proc):
            payload = client._call_responder({"requestType": "X", "payload": {}}, "cp")
        assert payload == {"events": ["e1", "e2"]}

    def test_request_is_sent_as_json_with_trailing_newline(self):
        proc = _fake_process(stdout=OK_LINE)
        request = {"requestType": "GET_USER_EVENTS", "payload": {"userId": "u1"}}
        with _patch_popen(proc):
            client._call_responder(request, "cp")
        sent = proc.communicate.call_args.args[0]
        assert sent.endswith("\n")
        assert json.loads(sent) == request

    def test_command_uses_given_classpath_and_main_class(self):
        proc = _fake_process(stdout=OK_LINE)
        with _patch_popen(proc) as popen:
            client._call_responder({"requestType": "X", "payload": {}}, "my-classpath")
        command = popen.call_args.args[0]
        assert command == ["java", "-cp", "my-classpath", client.RESPONDER_MAIN_CLASS]

    def test_error_status_raises_with_message(self):
        line = json.dumps({"status": "error", "error": "boom"}) + "\n"
        proc = _fake_process(stdout=line, returncode=0)
        with _patch_popen(proc), pytest.raises(SubprocessError, match="boom"):
            client._call_responder({}, "cp")

    def test_nonzero_exit_prefers_envelope_error(self):
        line = json.dumps({"status": "error", "error": "User x not found"}) + "\n"
        proc = _fake_process(stdout=line, returncode=1)
        with _patch_popen(proc), pytest.raises(SubprocessError, match="User x not found"):
            client._call_responder({}, "cp")

    def test_nonzero_exit_falls_back_to_stderr(self):
        proc = _fake_process(stdout="garbage", stderr="stack trace here", returncode=2)
        with _patch_popen(proc), pytest.raises(SubprocessError, match="stack trace here"):
            client._call_responder({}, "cp")

    def test_nonzero_exit_falls_back_to_exit_code(self):
        proc = _fake_process(stdout="", stderr="", returncode=3)
        with _patch_popen(proc), pytest.raises(SubprocessError, match="exited with code 3"):
            client._call_responder({}, "cp")

    def test_unparseable_stdout_with_zero_exit_raises(self):
        proc = _fake_process(stdout="not json at all", returncode=0)
        with _patch_popen(proc), pytest.raises(SubprocessError, match="no parseable response"):
            client._call_responder({}, "cp")

    def test_unexpected_status_raises(self):
        line = json.dumps({"status": "weird"}) + "\n"
        proc = _fake_process(stdout=line, returncode=0)
        with _patch_popen(proc), pytest.raises(SubprocessError, match="Unexpected response status"):
            client._call_responder({}, "cp")

    def test_timeout_kills_process_and_raises(self):
        proc = mock.MagicMock()
        proc.communicate.side_effect = subprocess.TimeoutExpired(cmd="java", timeout=30)
        with _patch_popen(proc), pytest.raises(SubprocessError, match="timed out"):
            client._call_responder({}, "cp")
        proc.kill.assert_called_once()

    def test_missing_java_raises(self):
        with mock.patch("bridge.client.subprocess.Popen", side_effect=FileNotFoundError):
            with pytest.raises(SubprocessError, match="Could not launch java"):
                client._call_responder({}, "cp")


class TestResolveClasspath:
    def test_missing_build_output_raises(self, tmp_path, monkeypatch):
        monkeypatch.setattr(client, "_CLASSES_DIR", tmp_path / "classes")
        monkeypatch.setattr(client, "_CLASSPATH_FILE", tmp_path / "cp.txt")
        with pytest.raises(SubprocessError, match="not built"):
            client._resolve_classpath()

    def test_present_build_joins_classes_and_deps(self, tmp_path, monkeypatch):
        classes = tmp_path / "classes"
        classes.mkdir()
        cp_file = tmp_path / "cp.txt"
        cp_file.write_text("/a.jar:/b.jar\n")
        monkeypatch.setattr(client, "_CLASSES_DIR", classes)
        monkeypatch.setattr(client, "_CLASSPATH_FILE", cp_file)
        result = client._resolve_classpath()
        assert result == os.pathsep.join([str(classes), "/a.jar:/b.jar"])


class TestGetUserEvents:
    def test_builds_request_and_returns_events(self, monkeypatch):
        monkeypatch.setattr(client, "_resolve_classpath", lambda: "fake-cp")
        # get_user_events parses the responder's event IDs into UUIDs, so the
        # canned events must be valid UUID strings.
        e1 = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
        e2 = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
        line = json.dumps({"status": "ok", "payload": {"events": [e1, e2]}}) + "\n"
        proc = _fake_process(stdout=line)
        with _patch_popen(proc) as popen:
            result = get_user_events("user-123")
        assert result == [uuid.UUID(e1), uuid.UUID(e2)]
        # The request carries the user id under the GET_USER_EVENTS envelope...
        sent = json.loads(proc.communicate.call_args.args[0])
        assert sent == {"requestType": "GET_USER_EVENTS", "payload": {"userId": "user-123"}}
        # ...and the resolved classpath is passed straight to java -cp.
        assert popen.call_args.args[0] == ["java", "-cp", "fake-cp", client.RESPONDER_MAIN_CLASS]

    def test_propagates_responder_error(self, monkeypatch):
        monkeypatch.setattr(client, "_resolve_classpath", lambda: "fake-cp")
        line = json.dumps({"status": "error", "error": "User u not found"}) + "\n"
        proc = _fake_process(stdout=line, returncode=1)
        with _patch_popen(proc), pytest.raises(SubprocessError, match="not found"):
            get_user_events("u")


class TestGetEventInfo:
    def test_builds_request_and_returns_payload(self, monkeypatch):
        monkeypatch.setattr(client, "_resolve_classpath", lambda: "fake-cp")
        # get_event_info returns the responder's payload verbatim (the caller,
        # not the client, parses host/start/category into typed values).
        payload = {
            "host": "22222222-2222-2222-2222-222222222222",
            "start": "2026-09-15T18:00:00",
            "category": "SOCIAL",
        }
        line = json.dumps({"status": "ok", "payload": payload}) + "\n"
        proc = _fake_process(stdout=line)
        with _patch_popen(proc) as popen:
            result = get_event_info("event-123")
        assert result == payload
        # The request carries the event id under the GET_EVENT_INFO envelope...
        sent = json.loads(proc.communicate.call_args.args[0])
        assert sent == {"requestType": "GET_EVENT_INFO", "payload": {"eventId": "event-123"}}
        # ...and the resolved classpath is passed straight to java -cp.
        assert popen.call_args.args[0] == ["java", "-cp", "fake-cp", client.RESPONDER_MAIN_CLASS]

    def test_propagates_responder_error(self, monkeypatch):
        monkeypatch.setattr(client, "_resolve_classpath", lambda: "fake-cp")
        line = json.dumps({"status": "error", "error": "Event e not found"}) + "\n"
        proc = _fake_process(stdout=line, returncode=1)
        with _patch_popen(proc), pytest.raises(SubprocessError, match="not found"):
            get_event_info("e")


class TestGetBatchEventInfo:
    # Two canned events, each with the same payload shape get_event_info returns.
    _INFO_A = {
        "host": "22222222-2222-2222-2222-222222222222",
        "start": "2026-09-15T18:00:00",
        "category": "SOCIAL",
    }
    _INFO_B = {
        "host": "33333333-3333-3333-3333-333333333333",
        "start": "2026-10-01T14:30:00",
        "category": "ACADEMIC",
    }

    def test_builds_request_and_returns_unwrapped_event_list(self, monkeypatch):
        monkeypatch.setattr(client, "_resolve_classpath", lambda: "fake-cp")
        e1 = uuid.UUID("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        e2 = uuid.UUID("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        # The responder wraps the per-event dicts in an ``events`` array; the
        # client unwraps it so callers get the list directly.
        line = (
            json.dumps({"status": "ok", "payload": {"events": [self._INFO_A, self._INFO_B]}})
            + "\n"
        )
        proc = _fake_process(stdout=line)
        with _patch_popen(proc) as popen:
            result = get_batch_event_info([e1, e2])
        assert result == [self._INFO_A, self._INFO_B]
        # The request carries the event ids (as strings) under the
        # GET_BATCH_EVENT_INFO envelope...
        sent = json.loads(proc.communicate.call_args.args[0])
        assert sent == {
            "requestType": "GET_BATCH_EVENT_INFO",
            "payload": {"eventIds": [str(e1), str(e2)]},
        }
        # ...and the resolved classpath is passed straight to java -cp.
        assert popen.call_args.args[0] == ["java", "-cp", "fake-cp", client.RESPONDER_MAIN_CLASS]

    def test_uuid_objects_are_serialised_to_strings(self, monkeypatch):
        # Regression guard: UUID objects are not JSON-serialisable, so the client
        # must stringify each id before building the request envelope.
        monkeypatch.setattr(client, "_resolve_classpath", lambda: "fake-cp")
        e1 = uuid.UUID("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        proc = _fake_process(
            stdout=json.dumps({"status": "ok", "payload": {"events": [self._INFO_A]}}) + "\n"
        )
        with _patch_popen(proc):
            get_batch_event_info([e1])
        sent = proc.communicate.call_args.args[0]
        # Round-trips through json.dumps without raising, and carries string ids.
        assert json.loads(sent)["payload"]["eventIds"] == [str(e1)]

    def test_empty_event_list_returns_empty_list(self, monkeypatch):
        monkeypatch.setattr(client, "_resolve_classpath", lambda: "fake-cp")
        proc = _fake_process(
            stdout=json.dumps({"status": "ok", "payload": {"events": []}}) + "\n"
        )
        with _patch_popen(proc) as popen:
            result = get_batch_event_info([])
        assert result == []
        sent = json.loads(proc.communicate.call_args.args[0])
        assert sent == {"requestType": "GET_BATCH_EVENT_INFO", "payload": {"eventIds": []}}
        assert popen.call_args.args[0] == ["java", "-cp", "fake-cp", client.RESPONDER_MAIN_CLASS]

    def test_single_event_returns_single_item_list(self, monkeypatch):
        monkeypatch.setattr(client, "_resolve_classpath", lambda: "fake-cp")
        e1 = uuid.UUID("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        proc = _fake_process(
            stdout=json.dumps({"status": "ok", "payload": {"events": [self._INFO_A]}}) + "\n"
        )
        with _patch_popen(proc):
            result = get_batch_event_info([e1])
        assert result == [self._INFO_A]

    def test_preserves_event_order(self, monkeypatch):
        monkeypatch.setattr(client, "_resolve_classpath", lambda: "fake-cp")
        e1 = uuid.UUID("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        e2 = uuid.UUID("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        # Responder returns B before A; the client must not reorder.
        proc = _fake_process(
            stdout=json.dumps({"status": "ok", "payload": {"events": [self._INFO_B, self._INFO_A]}})
            + "\n"
        )
        with _patch_popen(proc):
            result = get_batch_event_info([e2, e1])
        assert result == [self._INFO_B, self._INFO_A]

    def test_propagates_responder_error(self, monkeypatch):
        monkeypatch.setattr(client, "_resolve_classpath", lambda: "fake-cp")
        line = json.dumps({"status": "error", "error": "Event e not found"}) + "\n"
        proc = _fake_process(stdout=line, returncode=1)
        with _patch_popen(proc), pytest.raises(SubprocessError, match="not found"):
            get_batch_event_info([uuid.UUID("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")])

    def test_missing_build_output_raises_before_spawning(self, tmp_path, monkeypatch):
        # _resolve_classpath runs first, so an unbuilt event-service fails fast
        # without ever launching java.
        monkeypatch.setattr(client, "_CLASSES_DIR", tmp_path / "classes")
        monkeypatch.setattr(client, "_CLASSPATH_FILE", tmp_path / "cp.txt")
        with mock.patch("bridge.client.subprocess.Popen") as popen:
            with pytest.raises(SubprocessError, match="not built"):
                get_batch_event_info([uuid.UUID("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")])
        popen.assert_not_called()


class TestNotifyBadgeAwarded:
    def test_builds_request_and_returns_none(self, monkeypatch):
        monkeypatch.setattr(client, "_resolve_classpath", lambda: "fake-cp")
        # A successful notification has an empty payload; the client returns None.
        line = json.dumps({"status": "ok", "payload": {}}) + "\n"
        proc = _fake_process(stdout=line)
        with _patch_popen(proc) as popen:
            result = notify_badge_awarded("user-123", "First Event")
        assert result is None
        # The request carries the user id and badge name under the BADGE_AWARDED
        # envelope...
        sent = json.loads(proc.communicate.call_args.args[0])
        assert sent == {
            "requestType": "BADGE_AWARDED",
            "payload": {"userId": "user-123", "badgeName": "First Event"},
        }
        # ...and the resolved classpath is passed straight to java -cp.
        assert popen.call_args.args[0] == ["java", "-cp", "fake-cp", client.RESPONDER_MAIN_CLASS]

    def test_propagates_responder_error(self, monkeypatch):
        monkeypatch.setattr(client, "_resolve_classpath", lambda: "fake-cp")
        line = json.dumps({"status": "error", "error": "User u not found"}) + "\n"
        proc = _fake_process(stdout=line, returncode=1)
        with _patch_popen(proc), pytest.raises(SubprocessError, match="not found"):
            notify_badge_awarded("u", "First Event")


class TestParsingHelpers:
    def test_parse_last_line_reads_final_json_line(self):
        assert client._parse_last_line('junk\n{"a": 1}\n') == {"a": 1}

    def test_parse_last_line_handles_single_line(self):
        assert client._parse_last_line('{"a": 1}') == {"a": 1}

    @pytest.mark.parametrize("value", ["", "   ", "\n", "not json"])
    def test_parse_last_line_returns_none_when_unusable(self, value):
        assert client._parse_last_line(value) is None

    def test_error_of_extracts_message(self):
        assert client._error_of({"error": "x"}) == "x"

    @pytest.mark.parametrize("value", [{}, {"status": "ok"}, None])
    def test_error_of_returns_none_without_error_field(self, value):
        assert client._error_of(value) is None
