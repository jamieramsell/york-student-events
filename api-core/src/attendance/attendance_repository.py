"""In-memory persistence for ``Attendance`` entities.

Provides ``InMemoryAttendanceRepository``, a dictionary-backed implementation of
``repositories.IRepository`` keyed by the tuples of two ``uuid.UUID`` which form
the IDs of Attendance records, plus the package-internal ``_repository``
singleton injected into the service layer. This stands in for a database-backed
repository during early development.
"""

import datetime
import uuid

import attendance.base as base
import repositories

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
                CANNED_ATTENDEE_ID, CANNED_EVENT_ID, datetime.datetime.now()
            )
        )

# Variable used to inject an instance of a repository into attendance_service.
# Package-internal (single leading underscore): consumed by other modules in the
# attendance package, but not part of the package's public API. Do not remove
# unless changing the dependency!
#
# The bridge responder serves this singleton, so it defaults to the canned
# repository to give end-to-end tests deterministic data. The Python test suite
# swaps it for a bare InMemoryAttendanceRepository per test (see conftest.py),
# so the seeded record is only ever visible over the subprocess bridge.
_repository = InMemoryCannedAttendanceRepository()