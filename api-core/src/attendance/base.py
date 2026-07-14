import dataclasses
import datetime
import repositories
import uuid

# Ordered (attendee_id, event_id) pair representing the ID of an Attendance
# record
type AttendanceId = tuple[uuid.UUID, uuid.UUID]


def _generate_id(attendee_id: uuid.UUID, event_id: uuid.UUID) -> AttendanceId:
    """Convenience function used to parse the ID of an Attendance record."""
    return (attendee_id, event_id)


@dataclasses.dataclass(frozen = True)
class Attendance(repositories.IEntity[AttendanceId]):
    """
    Defines the core structure of a record of Attendance.

    Args:
        attendee_id: The user ID of the Student attending the Event.
        event_id: The ID of the Event.
        recorded_at: The datetime at which the Student's attendance was logged.
    """
    attendee_id: uuid.UUID
    event_id: uuid.UUID
    recorded_at: datetime.datetime
    
    def get_id(self) -> AttendanceId:
        return _generate_id(self.attendee_id, self.event_id)