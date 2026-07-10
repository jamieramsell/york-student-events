"""In-memory persistence for ``Attendance`` entities.

Provides ``InMemoryAttendanceRepository``, a dictionary-backed implementation of
``repositories.IRepository`` keyed by the tuples of two ``uuid.UUID`` which form
the IDs of Attendance records, plus the package-internal ``_repository``
singleton injected into the service layer. This stands in for a database-backed
repository during early development.
"""

import attendance.base as base
import repositories

class InMemoryAttendanceRepository(
    repositories.IRepository[base.AttendanceId, base.Attendance]
):
    """Dictionary backed repository for storing and retrieving Attendance
    records.
    
    Extends repositories.IRepository with ``tuple[uuid.UUID, uuid.UUID]`` as the
    managed type, providing standard CRUD operations scoped to the keys (formed
    of a 2-tuple of UUID keys) of Attendance records. Used for integration
    testing before implementing database-backed repositories.
    
    See Also:
        repositories.IRepository
    """

    def __init__(self):
        self.__dict: dict[base.AttendanceId, base.Attendance] = {}

    def save(self, entity: base.Attendance) -> None:
        self.__dict[entity.get_id()] = entity

    def delete(self, entity_id: base.AttendanceId) -> None:
        self.__dict.pop(entity_id)

    def find_by_id(
        self, entity_id: base.AttendanceId
    ) -> base.Attendance | None:
        return self.__dict.get(entity_id)

    def find_all(self) -> list[base.Attendance]:
        return list(self.__dict.values())

# Variable used to inject an instance of a repository into attendance_service.
# Package-internal (single leading underscore): consumed by other modules in the
# attendance package, but not part of the package's public API. Do not remove
# unless changing the dependency!
_repository = InMemoryAttendanceRepository()