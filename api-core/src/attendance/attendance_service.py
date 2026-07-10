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
    """Creates a new Attendance record, timestamped to the current time.

    Args:
        attendee_id: The ID of the Student attending the Event.
        event_id: The ID of the Event.

    Raises:
        ValueError: if the user's attendance to that event has already been
            recorded.

    See Also:
        Attendance
    """
    record_id = base._generate_id(attendee_id, event_id)
    existing_record = attendance_repository._repository.find_by_id(record_id)
    if existing_record is not None:
        raise ValueError("The user's attendance has already been recorded.")

    attendance_record = base.Attendance(
        attendee_id, event_id, datetime.datetime.now()
    )
    attendance_repository._repository.save(attendance_record)

    # Add activity.publish(attendee_id) here


def withdraw_attendance(attendee_id: uuid.UUID, event_id: uuid.UUID) -> None:
    """Removes the Attendance record for a student/event pair.

    Args:
        attendee_id: The ID of the Student attending the Event.
        event_id: The ID of the Event.

    Raises:
        ValueError: if no Attendance record exists for the pair.

    See Also:
        Attendance
    """
    try:
        attendance_repository._repository.delete(
            base._generate_id(attendee_id, event_id)
        )
    except KeyError:
        raise ValueError("No attendance record for the given event exists for"
                         + " the given user.")


def has_attended(attendee_id: uuid.UUID, event_id: uuid.UUID) -> bool:
    """Checks whether a student has an Attendance record for an event.

    Args:
        attendee_id: The ID of the Student attending the Event.
        event_id: The ID of the Event.

    Returns:
        Whether an Attendance record exists for the pair.
    """
    record_id = base._generate_id(attendee_id, event_id)
    return attendance_repository._repository.find_by_id(record_id) != None


def get_attendances(attendee_id: uuid.UUID) -> list[base.Attendance]:
    """Retrieves all Attendance records for a given student.

    Args:
        attendee_id: The ID of the Student whose attendances are retrieved.

    Returns:
        A list of the student's Attendance records; never None, may be empty.
    """
    all_attendance_records = attendance_repository._repository.find_all()
    user_attendances = [record for record in all_attendance_records
                        if record.attendee_id == attendee_id]
    return user_attendances