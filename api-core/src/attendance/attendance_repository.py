"""In-memory persistence for ``Attendance`` entities.

Provides ``InMemoryAttendanceRepository``, a dictionary-backed implementation of
``repositories.IRepository`` keyed by the tuples of two ``uuid.UUID`` which form
the IDs of Attendance records. This stands in for a database-backed repository
during early development.
"""
import datetime
import sqlalchemy
import typing
import uuid

import repositories
import repositories.sql
from attendance import base


class InMemoryAttendanceRepository(
    repositories.InMemoryRepository[base.AttendanceId, base.Attendance]
):
    """Dictionary backed repository for storing and retrieving Attendance
    records.
    
    Extends repositories.InMemoryRepository with ``tuple[uuid.UUID, uuid.UUID]``
    as the managed type, providing standard CRUD operations scoped to the keys
    (formed of a 2-tuple of UUID keys) of Attendance records. Used for
    integration testing before implementing database-backed repositories.
    
    See Also:
        repositories.IRepository
        repositories.InMemoryRepository
    """

# Canonical canned attendance record seeded into every
# InMemoryCannedAttendanceRepository. The attendee id matches the KNOWN_USER_ID
# used by the bridge integration tests; the pair is what those tests re-record to
# exercise the duplicate-rejection path.
CANNED_ATTENDEE_ID = uuid.UUID("11111111-1111-1111-1111-111111111111")
CANNED_EVENT_ID = uuid.UUID("22222222-2222-2222-2222-222222222222")


class InMemoryCannedAttendanceRepository(InMemoryAttendanceRepository):
    """In-memory attendance repository pre-seeded with a single canned record.

    Behaves exactly like ``InMemoryAttendanceRepository`` but starts populated
    with the ``(CANNED_ATTENDEE_ID, CANNED_EVENT_ID)`` attendance. This gives the
    bridge responder deterministic, functional state to serve during end-to-end
    testing: re-recording the canned pair surfaces the duplicate-attendance
    error, while any other pair records successfully.

    See Also:
        InMemoryAttendanceRepository
    """

    def __init__(self):
        super().__init__()
        self.save(
            base.Attendance(
                CANNED_ATTENDEE_ID,
                CANNED_EVENT_ID,
                datetime.datetime.now(datetime.timezone.utc),
            )
        )


class SQLAlchemyAttendanceRepository(
    repositories.sql.SqlAlchemyRepository[base.AttendanceId, base.Attendance]
):
    """Database backed repository for storing and retrieving Attendance
    records.
    
    Extends repositories.sql.SqlAlchemyRepository with
    ``tuple[uuid.UUID, uuid.UUID]`` as the managed type, providing standard CRUD
    operations scoped to the keys (formed of a 2-tuple of UUID keys) of
    Attendance records. 

    See Also:
        repositories.IRepository
        repositories.sql.SqlAlchemyRepository
    """
    @property
    def _table(self) -> sqlalchemy.Table:
        return repositories.sql.attendance
    

    def _id_predicate(
        self, entity_id: base.AttendanceId
    ) -> sqlalchemy.ColumnElement[bool]:
        return sqlalchemy.and_(self._table.c.attendee_id == entity_id[0],
                               self._table.c.event_id == entity_id[1])


    def _to_row(self, entity: base.Attendance) -> dict[str, typing.Any]:
        return {
            "attendee_id": entity.attendee_id,
            "event_id": entity.event_id,
            "recorded_at": entity.recorded_at
        }
    

    def _from_row(self, row: sqlalchemy.Row[typing.Any]) -> base.Attendance:
        attendee_id = row.attendee_id
        event_id = row.event_id
        recorded_at = row.recorded_at

        attendance_record = base.Attendance(attendee_id, event_id, recorded_at)
        return attendance_record