"""Subprocess bridge between api-core (Python) and event-service (Java).

This package wraps the JSON-over-stdio contract that lets the two services
invoke each other as child processes (see ``docs/subprocess-contract.md``):

- ``client``     — api-core spawns the Java responder and requests payloads from
  it.
- ``responder``  — api-core answers requests issued by event-service over
  standard input/output.

Stdlib only, in keeping with the project's no-dependencies convention.
"""

# Note that responder is not included in __init__.py, as it is an entry point
# from outside of the Python service, therefore doesn't need to be imported
# into any python files. For testing, simply import the file directly.

from bridge.client import (
    SubprocessError,
    get_batch_event_info,
    get_event_info,
    get_user_events,
    notify_badge_awarded,
)

__all__ = [
    "SubprocessError",
    "get_batch_event_info",
    "get_event_info",
    "get_user_events",
    "notify_badge_awarded"
]
