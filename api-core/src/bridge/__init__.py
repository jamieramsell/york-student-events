"""Subprocess bridge between api-core (Python) and event-service (Java).

This package wraps the JSON-over-stdio contract that lets the two services invoke
each other as child processes (see ``docs/docs/subprocess-contract.md``):

- ``subprocess_client``  — api-core spawns the Java responder and requests payloads
  from it.
- ``subprocess_bridge``  — api-core answers requests issued by event-service over
  standard input/output.

Stdlib only, in keeping with the project's no-dependencies convention.
"""

from bridge.subprocess_client import SubprocessError, get_user_events

__all__ = ["SubprocessError", "get_user_events"]
