"""Client for requesting payloads from the Java event-service over the
subprocess bridge.

This is the inverse of ``responder.py``: instead of event-service
spawning Python, api-core spawns the Java ``SubprocessResponder`` as a fresh
process per call, writes a JSON request envelope to its stdin, and reads a JSON
response envelope from its stdout. The envelope contract is documented in
``docs/subprocess-contract.md``.
"""

from __future__ import annotations

import json
import os
import pathlib
import subprocess
import uuid

RESPONDER_MAIN_CLASS = "york.studentevents.subprocess.SubprocessResponder"
_TIMEOUT_SECONDS = 30

# client.py lives at api-core/src/bridge/, so the repo root is three levels up.
_REPO_ROOT = pathlib.Path(__file__).resolve().parents[3]
_EVENT_SERVICE = _REPO_ROOT / "event-service"
_CLASSES_DIR = _EVENT_SERVICE / "target" / "classes"
_CLASSPATH_FILE = _EVENT_SERVICE / "target" / "cp.txt"


class SubprocessError(RuntimeError):
    """Raised when the Java responder returns an error envelope or exits
    non-zero."""


def get_user_events(user_id: uuid.UUID) -> list[uuid.UUID]:
    """Fetch the IDs of events a user is registered for from event-service.

    Args:
        user_id (uuid.UUID): UUID of the user whose events to fetch.

    Returns:
        list[uuid.UUID]: Event IDs the user is registered for (empty if none).

    Raises:
        SubprocessError: If event-service is not built, or the responder returns
            an error envelope, exits non-zero, times out, or returns an
            unparseable response.
    """
    user_id: str = str(user_id)
    classpath = _resolve_classpath()
    request = {"requestType": "GET_USER_EVENTS", "payload": {"userId": user_id}}

    # Get list of event IDs as strings, and return them as UUIDs
    list_of_events: list[str] = _call_responder(request, classpath)["events"]
    event_uuids: list[uuid.UUID] = []
    for event in list_of_events:
        event_uuids.append(uuid.UUID(event))

    return event_uuids


def get_event_info(event_id: uuid.UUID) -> dict[str, str]:
    """Fetch the properties of a given event from event-service, which is
    required to construct a ``badges.AttendedEvent`` object.

    Args:
        event_id (uuid.UUID): UUID of the event whose information to fetch.

    Returns:
        dict[str, str]: The properties of the event with the given ID.

    Raises:
        SubprocessError: If event-service is not built, or the responder returns
            an error envelope, exits non-zero, times out, or returns an
            unparseable response.
    """
    event_id: str = str(event_id)
    classpath = _resolve_classpath()
    request = {"requestType": "GET_EVENT_INFO",
               "payload": {"eventId": event_id}}

    event_properties: dict[str, str] = _call_responder(request, classpath)
    return event_properties


def get_batch_event_info(event_ids: list[uuid.UUID]) -> list[dict[str, str]]:
    """Fetch the properties of a given list of events from event-service, which
    is required to construct a series of ``badges.AttendedEvent`` objects.

    Args:
        event_ids (list[uuid.UUID]): the UUIDs of the events whose information
            to fetch.

    Returns:
        list[dict[str, str]]: The properties of the given events.

    Raises:
        SubprocessError: If event-service is not built, or the responder returns
            an error envelope, exits non-zero, times out, or returns an
            unparseable response.
    """
    classpath = _resolve_classpath()
    request = {
        "requestType": "GET_BATCH_EVENT_INFO",
        "payload": {"eventIds": [str(event_id) for event_id in event_ids]},
    }

    # The responder wraps the per-event info dicts in an ``events`` array (see
    # the GET_BATCH_EVENT_INFO row of docs/subprocess-contract.md); unwrap it so
    # callers get the list of event-info dicts directly, mirroring
    # ``get_user_events``.
    event_properties: list[dict[str, str]] = _call_responder(
        request, classpath
    )["events"]
    return event_properties


def notify_badge_awarded(user_id: uuid.UUID, badge_name: str) -> None:
    """Notify event-service that a user has just been awarded a badge.

    This is a fire-and-forget notification: event-service acknowledges it but
    returns no payload. Callers use it to tell event-service about badges
    ``api-core`` auto-awards, so it can (eventually) notify the student.

    Args:
        user_id (uuid.UUID): UUID of the user who was awarded the badge.
        badge_name (str): Display name of the badge that was awarded.

    Raises:
        SubprocessError: If event-service is not built, or the responder returns
            an error envelope, exits non-zero, times out, or returns an
            unparseable response.
    """
    classpath = _resolve_classpath()
    request = {
        "requestType": "BADGE_AWARDED",
        "payload": {"userId": str(user_id), "badgeName": badge_name},
    }

    _call_responder(request, classpath)


def _resolve_classpath() -> str:
    """Builds the classpath from the event-service build output.

    Returns:
        str: ``target/classes`` joined with the dependency classpath in
            ``target/cp.txt``.

    Raises:
        SubprocessError: If the build output is missing (event-service not
            compiled).
    """
    if not _CLASSES_DIR.is_dir() or not _CLASSPATH_FILE.is_file():
        raise SubprocessError(
            "event-service is not built. Run `./mvnw compile` from the"
            + f" event-service directory to produce {_CLASSES_DIR} and"
            + f" {_CLASSPATH_FILE}."
        )
    
    dependency_classpath = _CLASSPATH_FILE.read_text().strip()
    return os.pathsep.join([str(_CLASSES_DIR), dependency_classpath])

def _call_responder(request: dict, classpath: str) -> dict:
    """Spawns the Java responder, sends the request, and returns the response
    payload."""
    command = ["java", "-cp", classpath, RESPONDER_MAIN_CLASS]
    try:
        process = subprocess.Popen(
            command,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )
    except FileNotFoundError as exc: # `java` not on PATH
        raise SubprocessError("Could not launch java;"
                              + " is the JDK on PATH?") from exc

    try:
        # communicate() drains stdin and stdout together, avoiding a deadlock
        # where both processes block on full pipe buffers.
        stdout, stderr = process.communicate(
            json.dumps(request) + "\n", timeout=_TIMEOUT_SECONDS
        )
    except subprocess.TimeoutExpired as exc:
        process.kill()
        raise SubprocessError("Java responder timed out.") from exc

    parsed = _parse_last_line(stdout)

    if process.returncode != 0:
        message = (
            _error_of(parsed)
            or (stderr.strip() if stderr else "")
            or f"Java responder exited with code {process.returncode}."
        )
        raise SubprocessError(message)

    if parsed is None:
        raise SubprocessError("Java responder returned no parseable response.")
    if parsed.get("status") == "error":
        raise SubprocessError(_error_of(parsed) or "Unknown responder error.")
    if parsed.get("status") != "ok":
        raise SubprocessError("Unexpected response status:"
                              + f" {parsed.get('status')!r}.")

    return parsed.get("payload", {})

def _parse_last_line(stdout: str) -> dict | None:
    """Parses the last non-empty stdout line as JSON, or returns None if
    unparseable."""
    if not stdout or not stdout.strip():
        return None
    try:
        return json.loads(stdout.strip().splitlines()[-1])
    except (json.JSONDecodeError, ValueError):
        return None

def _error_of(parsed: dict | None) -> str | None:
    """Returns the ``error`` message from a parsed envelope, if present."""
    return parsed.get("error") if isinstance(parsed, dict) else None