"""Attendance slice for the api-core service.

This package handles the recording of attendance of students to events.

The public surface is the ``Attendance`` entity, the attendance repositories,
and the service-level operations callers use to drive attendance operations.
"""
from attendance.attendance_repository import (
    InMemoryAttendanceRepository,
    SQLAlchemyAttendanceRepository,
)
from attendance.attendance_service import AttendanceRepository, AttendanceService
from attendance.base import Attendance

__all__ = [
    "Attendance",
    "AttendanceRepository",
    "AttendanceService",
    "InMemoryAttendanceRepository",
    "SQLAlchemyAttendanceRepository"
]
