from __future__ import annotations

import datetime
import uuid
import pytest
import sqlalchemy

import attendance
import badges
import badges.predicates
import friends
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


def _badge(id: uuid.UUID | None = None) -> badges.Badge:
    
    id = id if id is not None else uuid.uuid4()
    
    return badges.Badge(
        id,
        "BadgeName",
        "BadgeDesc",
        badges.predicates.MinMessagesSent(5)
    )


def _friendship(user_id: uuid.UUID | None = None,
                friend_id: uuid.UUID | None = None) -> friends.Friendship:
    
    user_id = user_id if user_id is not None else uuid.uuid4()
    friend_id = friend_id if friend_id is not None else uuid.uuid4()

    return friends.Friendship(
        user_id=user_id,
        friend_id=friend_id,
        friendship_status=friends.FriendshipStatus.ACCEPTED,

        # tz-aware UTC, like the canned repo
        created_at=datetime.datetime.now(datetime.timezone.utc)
    )


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------
@pytest.fixture
def engine(sqlite_engine: sqlalchemy.Engine):
    """A fresh, shared in-memory SQLite engine with the real tables linked.

    ``StaticPool`` keeps every connection pointed at the same in-memory
    database, meaning a value written through one ``engine.begin()`` block is
    visible to the next, as well as to a second repository instance built on the
    same engine.
    """

    sql.metadata.create_all(sqlite_engine)  # the real schema
    return sqlite_engine


@pytest.fixture
def attendance_repo(
    engine: sqlalchemy.Engine
) -> attendance.SQLAlchemyAttendanceRepository:
    return attendance.SQLAlchemyAttendanceRepository(engine)


@pytest.fixture
def awarded_badge_repo(
    engine: sqlalchemy.Engine
) -> badges.SQLAlchemyAwardedBadgeRepository:
    return badges.SQLAlchemyAwardedBadgeRepository(engine)


@pytest.fixture
def badge_repo(
    engine: sqlalchemy.Engine
) -> badges.SQLAlchemyBadgeRepository:
    return badges.SQLAlchemyBadgeRepository(engine)


@pytest.fixture
def friendship_repo(
    engine: sqlalchemy.Engine
) -> friends.SQLAlchemyFriendshipRepository:
    return friends.SQLAlchemyFriendshipRepository(engine)


# ---------------------------------------------------------------------------
# Mapping: methods successfully map specific records to and from the database
# ---------------------------------------------------------------------------
class TestAttendanceMapping:

    def test_round_trip(self, attendance_repo: attendance.AttendanceRepository):
        attendance_record = _attendance()
        attendance_repo.save(attendance_record)

        lookup = attendance_repo.find_by_id(attendance_record.get_id())

        # Ensure the record was found and has been handled correctly during
        # save and find
        assert lookup is not None 
        assert lookup == attendance_record
        

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

        # Ensure the record was found and has been handled correctly during
        # save and find
        assert lookup is not None 
        assert lookup == award_record
        

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


class TestBadgeMapping:

    def test_round_trip(self, badge_repo: badges.BadgeRepository):
        badge_record = _badge()
        badge_repo.save(badge_record)

        lookup = badge_repo.find_by_id(badge_record.get_id())

        # Ensure that the record was found
        assert lookup is not None 
        assert lookup == badge_record


class TestFriendshipMapping:

    def test_round_trip(
        self,
        friendship_repo: friends.FriendshipRepository
    ):
        friendship_record = _friendship()
        friendship_repo.save(friendship_record)

        lookup = friendship_repo.find_by_id(friendship_record.get_id())

        # Ensure the record was found and has been handled correctly during
        # save and find
        assert lookup is not None 
        assert lookup == friendship_record
        

    def test_entities_sharing_user_id_are_distinct(
        self,
        friendship_repo: friends.FriendshipRepository
    ):
        record1 = _friendship()
        record2 = _friendship(user_id=record1.user_id)

        friendship_repo.save(record1)
        friendship_repo.save(record2)

        # Ensure that both records were saved and are able to be retrieved,
        # despite sharing a key component
        assert len(friendship_repo.find_all()) == 2

        lookup1 = friendship_repo.find_by_id(record1.get_id())
        lookup2 = friendship_repo.find_by_id(record2.get_id())
        assert lookup1 is not None
        assert lookup2 is not None
        assert lookup1.get_id() == record1.get_id()
        assert lookup2.get_id() == record2.get_id()


    def test_entities_sharing_friend_id_are_distinct(
        self,
        friendship_repo: friends.FriendshipRepository
    ):
        record1 = _friendship()
        record2 = _friendship(friend_id=record1.friend_id)

        friendship_repo.save(record1)
        friendship_repo.save(record2)

        # Ensure that both records were saved and are able to be retrieved,
        # despite sharing a key component
        assert len(friendship_repo.find_all()) == 2

        lookup1 = friendship_repo.find_by_id(record1.get_id())
        lookup2 = friendship_repo.find_by_id(record2.get_id())
        assert lookup1 is not None
        assert lookup2 is not None
        assert lookup1.get_id() == record1.get_id()
        assert lookup2.get_id() == record2.get_id()

    
    def test_entities_where_record1_friend_equals_record2_user_are_distinct(
        self,
        friendship_repo: friends.FriendshipRepository
    ):
        record1 = _friendship()
        record2 = _friendship(user_id=record1.friend_id)

        friendship_repo.save(record1)
        friendship_repo.save(record2)

        # Ensure that both records were saved and are able to be retrieved,
        # despite sharing a key component
        assert len(friendship_repo.find_all()) == 2

        lookup1 = friendship_repo.find_by_id(record1.get_id())
        lookup2 = friendship_repo.find_by_id(record2.get_id())
        assert lookup1 is not None
        assert lookup2 is not None
        assert lookup1.get_id() == record1.get_id()
        assert lookup2.get_id() == record2.get_id()


    def test_entities_where_record1_user_equals_record2_friend_are_distinct(
        self,
        friendship_repo: friends.FriendshipRepository
    ):
        record1 = _friendship()
        record2 = _friendship(friend_id=record1.user_id)

        friendship_repo.save(record1)
        friendship_repo.save(record2)

        # Ensure that both records were saved and are able to be retrieved,
        # despite sharing a key component
        assert len(friendship_repo.find_all()) == 2

        lookup1 = friendship_repo.find_by_id(record1.get_id())
        lookup2 = friendship_repo.find_by_id(record2.get_id())
        assert lookup1 is not None
        assert lookup2 is not None
        assert lookup1.get_id() == record1.get_id()
        assert lookup2.get_id() == record2.get_id()

    def test_entities_where_user_and_friend_are_reversed_are_not_distinct(
        self,
        friendship_repo: friends.FriendshipRepository
    ):
        record1 = _friendship()
        record2 = _friendship(user_id=record1.friend_id,
                              friend_id=record1.user_id)

        friendship_repo.save(record1)
        friendship_repo.save(record2)

        # Ensure that only one record was saved and is able to be retrieved,
        # despite key components being reversed
        assert len(friendship_repo.find_all()) == 1

        lookup1 = friendship_repo.find_by_id(record1.get_id())
        lookup2 = friendship_repo.find_by_id(record2.get_id())
        assert lookup1 is not None
        assert lookup2 is not None
        assert lookup1.get_id() == record1.get_id()
        assert lookup2.get_id() == record2.get_id()
        assert lookup1 == lookup2
