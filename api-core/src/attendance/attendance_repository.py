"""In-memory persistence for ``Attendance`` entities.

Provides ``InMemoryAttendanceRepository``, a dictionary-backed implementation of
``repositories.IRepository``, and ``SQLAlchemyAttendanceRepository``, which are
keyed by the tuples of two ``uuid.UUID`` which form the IDs of Attendance
records.
"""
import typing

import sqlalchemy

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
    (formed of a 2-tuple of UUID keys) of Attendance records. Used for testing
    purposes only.
    
    See Also:
        repositories.IRepository
        repositories.InMemoryRepository
    """


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