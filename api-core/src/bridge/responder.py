"""Responder answering subprocess requests issued by the Java event-service.

This is the inverse of ``client.py``: event-service spawns this module as a
fresh process per call, writes a JSON request envelope to its stdin, and reads a
JSON response envelope from its stdout. Each line of stdin is one request; the
``MessageHandlerFactory`` routes it to a handler by ``requestType``, and the
result is written back as an ``ok`` or ``error`` envelope. The envelope contract
is documented in ``docs/subprocess-contract.md``.

Stdlib only, in keeping with the project's no-dependencies convention.
"""
import collections.abc
import json
import os
import sys
import uuid

# responder.py is launched as a standalone subprocess (by event-service and by
# the test suite), so the api-core ``src`` root is not guaranteed to be on
# ``sys.path``. Anchor it relative to this file so the api-core packages below
# resolve regardless of the working directory the process is launched from.
_SRC = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if _SRC not in sys.path:
    sys.path.insert(0, _SRC)

import bootstrap
import bootstrap_sql

_services: bootstrap.Services | None = None

def _compose_services() -> bootstrap.Services:
    """Composes the responder's service graph, selecting the backend from the
    environment.

    Backend selection follows the contract in ``docs/subprocess-contract.md``
    §7:

    - By default, the graph is composed over the shared database via
      ``bootstrap_sql()``, which reads ``DATABASE_URL``. This is the only
      production configuration, where the responder persists to and reads back
      from the database, which is what makes the per-call subprocess model
      stateful.
    - If ``YSE_BRIDGE_INMEMORY`` is set (to any non-empty value), a throwaway
      in-memory graph is composed instead, and ``DATABASE_URL`` is not required.
      This is a test-only switch used so that the responder can start without a
      database in tests (e.g. the Java-to-Python integration tests).

    Returns:
        A freshly composed ``Services`` graph. The database-backed graph
        registers the activity-driven auto-award listener (``register=True``);
        the in-memory test graph does not (``register=False``).

    Raises:
        ValueError: if neither ``YSE_BRIDGE_INMEMORY`` nor ``DATABASE_URL`` is
            set. A misconfigured bridge fails loudly as opposed to serving an
            empty, non-persistent graph.
    """
    if os.getenv("YSE_BRIDGE_INMEMORY"):
        return bootstrap.bootstrap(register=False)   # unit-test surface only
    return bootstrap_sql.bootstrap_sql()             # SQL repos, register=True; raises w/o DATABASE_URL


def _ensure_services() -> None:
    """Lazily composes the service graph into the module-level ``_services``,
    once.

    Composition is deferred until a request actually needs the services, so that
    the envelope-only paths (eg malformed JSON, an unknown or unowned
    ``requestType``, or a missing ``requestType``) never require a backend,
    meaning that importing this module has no side effects. The in-process tests
    inject their own graph by assigning ``_services`` directly, and this call
    then leaves it untouched.

    If ``_services`` is already set (either having been composed by an earlier
    request, or injected by a test), then the method call is a no-op. Otherwise,
    ``_services`` is composed via ``_compose_services()`` and memoised for the
    remainder of the process.

    Raises:
        ValueError: propagated from ``_compose_services()`` when no backend is
            configured (see that function).
    """
    global _services
    if _services is None:
        _services = _compose_services()


# Type alias of a Payload passed to a handler, formed of str keys, and str
# elements
type IncomingPayload = dict[str, str]

# Type alias of a Payload returned by a handler, formed of str keys, and
# list[str] elements
type OutgoingPayload = dict[str, list[str]]

# Type alias of a callable Handler, which accepts a str as a parameter, and
# returns a Payload
type Handler = collections.abc.Callable[[IncomingPayload], OutgoingPayload]


def get_user_badges(payload: IncomingPayload) -> OutgoingPayload:
    user_id = uuid.UUID(payload["userId"])
    return {
        "badges": [str(badge.get_id()) for badge
                   in _services.badge_service.get_user_badges(user_id)]
    }


def get_user_friends(payload: IncomingPayload) -> OutgoingPayload:
    user_id = uuid.UUID(payload["userId"])
    return {
        "friends": [str(friend_id) for friend_id
                    in _services.friendship_service.get_friends(user_id)]
    }


def award_badge(payload: IncomingPayload) -> OutgoingPayload:
    user_id = uuid.UUID(payload["userId"])
    badge_id = uuid.UUID(payload["badgeId"])
    _services.badge_service.award_badge(user_id, badge_id)
    return {}


def get_recommended_events(payload: IncomingPayload) -> OutgoingPayload:
    user_id = uuid.UUID(payload["userId"])
    return {
        "events": [str(event_id) for event_id
                   in _services.recommendations_service.get_recommended_events(user_id)]
    }


def record_attendance(payload: IncomingPayload) -> OutgoingPayload:
    attendee_id = uuid.UUID(payload["userId"])
    event_id = uuid.UUID(payload["eventId"])
    _services.attendance_service.record_attendance(attendee_id, event_id)
    return {}


def get_recommended_friends(payload: IncomingPayload) -> OutgoingPayload:
    user_id = uuid.UUID(payload["userId"])
    return {
        "friends": [str(recommended_friend_id) for recommended_friend_id
                     in _services.recommendations_service.find_new_friends(user_id)]
    }


class MessageHandlerFactory:
    """Routes incoming messages to their corresponding handler functions.

    Attributes:
        _handlers (dict[str, Callable): An internal mapping of supported
            message types and their handler functions.
    """
    def __init__(self):
        self._handlers: dict[str, Handler] = {
            "GET_USER_BADGES": get_user_badges,
            "GET_USER_FRIENDS": get_user_friends,
            "AWARD_BADGE": award_badge,
            "GET_RECOMMENDED_EVENTS": get_recommended_events,
            "RECORD_ATTENDANCE": record_attendance,
            "GET_RECOMMENDED_FRIENDS": get_recommended_friends
        }

    def get_handler(self, message_type: str) -> Handler:
        """Fetches the correct message handler from the message type provided.

        Args:
            message_type (str): String defining the type of request being
                passed. Currently supported types are:
                - `GET_USER_BADGES`,
                - `GET_USER_FRIENDS`,
                - `AWARD_BADGE`,
                - `GET_RECOMMENDED_EVENTS`,
                - `RECORD_ATTENDANCE`,
                - `GET_RECOMMENDED_FRIENDS`

        Raises:
            ValueError: If the message type is unknown, or doesn't have a
                corresponding handler.

        Returns:
            Callable: The function or method assigned to handle the
                message type.
        """
        handler = self._handlers.get(message_type)
        if not handler:
            raise ValueError(f"Unable to find handler for: `{message_type}`.")
        return handler


def main():
    factory = MessageHandlerFactory()

    for line in sys.stdin:

        line = line.strip()
        if not line:
            continue

        try:
            request = json.loads(line)
            msg_type = request.get("requestType")
            payload = request.get("payload", {})

            if not msg_type:
                raise ValueError("Missing 'requestType' field.")

            handler = factory.get_handler(msg_type)
            _ensure_services()
            result_payload = handler(payload)

            response: dict[str, str | OutgoingPayload] = {
                "status": "ok",
                "payload": result_payload,
            }

        except json.JSONDecodeError:
            response = {
                "status": "error",
                "error": "Incorrectly formatted json."
            }

        except Exception as e:  # noqa: BLE001 — deliberate catch-all: any handler
            # failure must be turned into an error envelope rather than crashing
            # the subprocess and breaking the stdio contract with event-service.
            response = {
                "status": "error",
                "error": str(e)
            }

        sys.stdout.write(json.dumps(response) + "\n")
        sys.stdout.flush()

        if response["status"] == "error":
            sys.exit(1)

if __name__ == "__main__":
    main()