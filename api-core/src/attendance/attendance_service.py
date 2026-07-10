"""Service-level operations for the attendance slice.

Exposes the attendance use cases callers depend on, such as recording a
student's attendance at an event, removing an attendance record, and querying
whether a student attended a given event or which students attended it.
Orchestrates the ``Attendance`` domain model from ``base`` with the in-memory
repository, keeping persistence details out of callers.
"""

import attendance.base as base
import attendance.attendance_repository as attendance_repository
import datetime
import uuid

def record_attendance(attendee_id: uuid.UUID, event_id: uuid.UUID) -> None:
    attendance_record = base.Attendance(
        attendee_id, event_id, datetime.datetime.now()
    )

    attendance_repository._repository.save(attendance_record)


def withdraw_attendance(attendee_id: uuid.UUID, event_id: uuid.UUID) -> None:
    attendance_repository._repository.delete(
        base._generate_id(attendee_id, event_id)
    )


def has_attended(attendee_id: uuid.UUID, event_id: uuid.UUID) -> bool:
    record_id = base._generate_id(attendee_id, event_id)
    return attendance_repository._repository.find_by_id(record_id) != None


def get_attendances(attendee_id: uuid.UUID) -> list[base.Attendance]:
    all_attendance_records = attendance_repository._repository.find_all()
    user_attendances = [record for record in all_attendance_records
                        if record.attendee_id == attendee_id]
    return user_attendances