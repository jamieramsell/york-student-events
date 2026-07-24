from __future__ import annotations

import datetime
import uuid
import pytest
import sqlalchemy

import attendance
import badges
from repositories import sql


# ---------------------------------------------------------------------------
# Record Constructors
# ---------------------------------------------------------------------------
def _attendance(attendee_id: uuid.UUID | None = None,
                event_id: uuid.UUID | None = None) -> attendance.Attendance:
    
    attendee_id = attendee_id if attendee_id is not None else uuid.uuid4()
    event_id = event_id if event_id is not None else uuid.uuid4()

    return attendance.Attendance(
        attendee_id,
        event_id,

        # tz-aware UTC, like the canned repo
        datetime.datetime.now(datetime.timezone.utc),   
    )


def _awarded_badge(user_id: uuid.UUID | None = None,
                   badge_id: uuid.UUID | None = None) -> badges.AwardedBadge:
    
    user_id = user_id if user_id is not None else uuid.uuid4()
    badge_id = badge_id if badge_id is not None else uuid.uuid4()

    return badges.AwardedBadge(
        user_id,
        badge_id,

        # tz-aware UTC, like the canned repo
        datetime.datetime.now(datetime.timezone.utc),   
    )


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------
@pytest.fixture
def engine():
    """A fresh, shared in-memory SQLite engine with the real tables linked.

    ``StaticPool`` keeps every connection pointed at the same in-memory
    database, meaning a value written through one ``engine.begin()`` block is
    visible to the next, as well as to a second repository instance built on the
    same engine.
    """

    engine = sqlalchemy.create_engine(
        "sqlite://",
        connect_args={"check_same_thread": False},
        poolclass=sqlalchemy.StaticPool,
    )
    sql.metadata.create_all(engine)
    yield engine
    engine.dispose()


@pytest.fixture
def attendance_repo(
    engine: sqlalchemy.Engine
) -> attendance.SQLAlchemyAttendanceRepository:
    return attendance.SQLAlchemyAttendanceRepository(engine)


# ---------------------------------------------------------------------------
# Mapping: methods successfully map specific records to and from the database
# ---------------------------------------------------------------------------
class TestAttendanceMapping:

    def test_round_trip(self, attendance_repo: attendance.AttendanceRepository):
        attendance_record = _attendance()
        attendance_repo.save(attendance_record)

        lookup = attendance_repo.find_by_id(attendance_record.get_id())

        assert lookup is not None # Ensure the record was found

        # Ensure that the composite key has been handled correctly
        assert lookup.get_id() == attendance_record.get_id()
        assert lookup.attendee_id == attendance_record.attendee_id
        assert lookup.event_id == attendance_record.event_id

        # Ensure that the datetime has been handled correctly. Note that tzinfo
        # has to be manually handled due to tests using SQLite, which does not
        # store timezone info; this is not the case for the real implementation,
        # which uses postgreSQL.
        assert (lookup.recorded_at.replace(tzinfo=datetime.timezone.utc)
                == attendance_record.recorded_at)
        

    def test_entities_sharing_attendee_id_are_distinct(
        self,
        attendance_repo: attendance.AttendanceRepository
    ):
        record1 = _attendance()
        record2 = _attendance(attendee_id=record1.attendee_id)

        attendance_repo.save(record1)
        attendance_repo.save(record2)

        # Ensure that both records were saved and are able to be retrieved,
        # despite sharing a key component
        assert len(attendance_repo.find_all()) == 2

        lookup1 = attendance_repo.find_by_id(record1.get_id())
        lookup2 = attendance_repo.find_by_id(record2.get_id())
        assert lookup1 is not None
        assert lookup2 is not None
        assert lookup1.get_id() == record1.get_id()
        assert lookup2.get_id() == record2.get_id()


    def test_entities_sharing_event_id_are_distinct(
        self,
        attendance_repo: attendance.AttendanceRepository
    ):
        record1 = _attendance()
        record2 = _attendance(event_id=record1.event_id)

        attendance_repo.save(record1)
        attendance_repo.save(record2)

        # Ensure that both records were saved and are able to be retrieved,
        # despite sharing a key component
        assert len(attendance_repo.find_all()) == 2
        
        lookup1 = attendance_repo.find_by_id(record1.get_id())
        lookup2 = attendance_repo.find_by_id(record2.get_id())
        assert lookup1 is not None
        assert lookup2 is not None
        assert lookup1.get_id() == record1.get_id()
        assert lookup2.get_id() == record2.get_id()


class TestAwardedBadgeMapping:

    def test_round_trip(
        self,
        awarded_badge_repo: badges.AwardedBadgeRepository
    ):
        award_record = _awarded_badge()
        awarded_badge_repo.save(award_record)

        lookup = awarded_badge_repo.find_by_id(award_record.get_id())

        assert lookup is not None # Ensure the record was found

        # Ensure that the composite key has been handled correctly
        assert lookup.get_id() == award_record.get_id()
        assert lookup.user_id == award_record.user_id
        assert lookup.badge_id == award_record.badge_id

        # Ensure that the datetime has been handled correctly. Note that tzinfo
        # has to be manually handled due to tests using SQLite, which does not
        # store timezone info; this is not the case for the real implementation,
        # which uses postgreSQL.
        assert (lookup.awarded_at.replace(tzinfo=datetime.timezone.utc)
                == award_record.awarded_at)
        

    def test_entities_sharing_user_id_are_distinct(
        self,
        awarded_badge_repo: badges.AwardedBadgeRepository
    ):
        record1 = _awarded_badge()
        record2 = _awarded_badge(user_id=record1.user_id)

        awarded_badge_repo.save(record1)
        awarded_badge_repo.save(record2)

        # Ensure that both records were saved and are able to be retrieved,
        # despite sharing a key component
        assert len(awarded_badge_repo.find_all()) == 2

        lookup1 = awarded_badge_repo.find_by_id(record1.get_id())
        lookup2 = awarded_badge_repo.find_by_id(record2.get_id())
        assert lookup1 is not None
        assert lookup2 is not None
        assert lookup1.get_id() == record1.get_id()
        assert lookup2.get_id() == record2.get_id()


    def test_entities_sharing_badge_id_are_distinct(
        self,
        awarded_badge_repo: badges.AwardedBadgeRepository
    ):
        record1 = _awarded_badge()
        record2 = _awarded_badge(badge_id=record1.badge_id)

        awarded_badge_repo.save(record1)
        awarded_badge_repo.save(record2)

        # Ensure that both records were saved and are able to be retrieved,
        # despite sharing a key component
        assert len(awarded_badge_repo.find_all()) == 2
        
        lookup1 = awarded_badge_repo.find_by_id(record1.get_id())
        lookup2 = awarded_badge_repo.find_by_id(record2.get_id())
        assert lookup1 is not None
        assert lookup2 is not None
        assert lookup1.get_id() == record1.get_id()
        assert lookup2.get_id() == record2.get_id()