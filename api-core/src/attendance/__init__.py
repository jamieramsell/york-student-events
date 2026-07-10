"""Attendance slice for the api-core service.

This package handles the recording of attendance of students to events. It
builds on the generic ``repositories`` abstraction, storing ``Attendance``
entities in an in-memory repository today, remaining open to a database-backed
backend later.

The public surface is the ``Attendance`` entity, the in-memory repository, and
the service-level operations callers use to drive attendance operations.
"""

from attendance.base import Attendance

__all__ = [
    "Attendance"
]
