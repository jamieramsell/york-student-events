"""Cross-instance persistence tests for the SQLAlchemy repository backend (#99).

Every other test in the suite reads back through the *same* service/repository
graph that performed the write, so a passing assertion cannot, on its own,
distinguish "the value was persisted to the database" from "the value is still
cached in the repository object". These tests close that gap.

Each test follows one shape:

    1. Build a *writer* service over a fresh SQLAlchemy repository on a shared
       in-memory SQLite engine, and mutate state through the service API.
    2. Build a *second, independent* service over a *fresh* repository on the
       **same** engine.
    3. Assert the state reads back through the reader.

Because the reader is a distinct object graph sharing nothing with the writer
but the ``Engine``, a value it can see can only have come from the database.
``sqlite_engine``'s ``StaticPool`` is what lets two instances address one
in-memory database; without it the reader would open a fresh, empty database and
every assertion here would fail. This is the repository-level guarantee of
``test_sql_repository.py::TestPersistenceAcrossInstances`` lifted up to the
service layer that #99 asks for.

The autouse ``compose_services`` fixture parametrizes the whole suite over
``["memory", "sql"]``; these tests build their own SQL instances and ignore that
graph, so both parameter runs execute identically. The duplicate ``[memory]`` /
``[sql]`` labels are therefore redundant but harmless.
"""
from __future__ import annotations

import uuid

import pytest
import sqlalchemy

import attendance
import badges
import badges.predicates
import friends
from repositories import sql


@pytest.fixture
def engine(sqlite_engine: sqlalchemy.Engine) -> sqlalchemy.Engine:
    """The shared in-memory engine with the real schema created on it.

    ``sqlite_engine`` (from conftest) yields a bare engine, so the real tables
    must be created here before any repository can bind to them.
    """
    sql.metadata.create_all(sqlite_engine)
    return sqlite_engine


# ---------------------------------------------------------------------------
# Friendships
# ---------------------------------------------------------------------------
class TestFriendshipPersistence:

    def test_accepted_friendship_survives_a_fresh_instance(
        self, engine: sqlalchemy.Engine
    ):
        alice, bob = uuid.uuid4(), uuid.uuid4()

        writer = friends.FriendshipService(
            friends.SQLAlchemyFriendshipRepository(engine)
        )
        writer.send_friend_request(alice, bob)
        writer.accept_friend_request(alice, bob)

        # A brand-new service over a brand-new repository, same database.
        reader = friends.FriendshipService(
            friends.SQLAlchemyFriendshipRepository(engine)
        )
        assert reader.is_friend(alice, bob)
        assert bob in reader.get_friends(alice)
        assert alice in reader.get_friends(bob)

    def test_friend_removal_survives_a_fresh_instance(
        self, engine: sqlalchemy.Engine
    ):
        alice, bob = uuid.uuid4(), uuid.uuid4()

        writer = friends.FriendshipService(
            friends.SQLAlchemyFriendshipRepository(engine)
        )
        writer.send_friend_request(alice, bob)
        writer.accept_friend_request(alice, bob)
        writer.remove_friend(alice, bob)

        # The delete must have reached the database, not just the writer's view.
        reader = friends.FriendshipService(
            friends.SQLAlchemyFriendshipRepository(engine)
        )
        assert not reader.is_friend(alice, bob)
        assert bob not in reader.get_friends(alice)

    def test_pending_request_survives_a_fresh_instance(
        self, engine: sqlalchemy.Engine
    ):
        alice, bob = uuid.uuid4(), uuid.uuid4()

        writer = friends.FriendshipService(
            friends.SQLAlchemyFriendshipRepository(engine)
        )
        writer.send_friend_request(alice, bob)

        # A pending request is not yet a friendship, but it must persist so a
        # fresh instance can accept it.
        reader = friends.FriendshipService(
            friends.SQLAlchemyFriendshipRepository(engine)
        )
        assert not reader.is_friend(alice, bob)
        assert alice in reader.get_pending_requests(bob)


# ---------------------------------------------------------------------------
# Badges
# ---------------------------------------------------------------------------
class TestBadgePersistence:

    def test_awarded_badge_survives_a_fresh_instance(
        self, engine: sqlalchemy.Engine
    ):
        user = uuid.uuid4()

        # The badge definition and the award live in two separate tables/repos;
        # both must persist for the reader to resolve the award back to a badge.
        writer = badges.BadgeService(
            badges.SQLAlchemyBadgeRepository(engine),
            badges.SQLAlchemyAwardedBadgeRepository(engine),
        )
        badge = writer.create_badge(
            "Socialite",
            "Attend five events",
            badges.predicates.MinEventsAttended(5),
        )
        writer.award_badge(user, badge.get_id())

        reader = badges.BadgeService(
            badges.SQLAlchemyBadgeRepository(engine),
            badges.SQLAlchemyAwardedBadgeRepository(engine),
        )
        earned = reader.get_user_badges(user)
        assert badge in earned

    def test_revoked_badge_survives_a_fresh_instance(
        self, engine: sqlalchemy.Engine
    ):
        user = uuid.uuid4()

        writer = badges.BadgeService(
            badges.SQLAlchemyBadgeRepository(engine),
            badges.SQLAlchemyAwardedBadgeRepository(engine),
        )
        badge = writer.create_badge(
            "Socialite",
            "Attend five events",
            badges.predicates.MinEventsAttended(5),
        )
        writer.award_badge(user, badge.get_id())
        writer.revoke_badge(user, badge.get_id())

        reader = badges.BadgeService(
            badges.SQLAlchemyBadgeRepository(engine),
            badges.SQLAlchemyAwardedBadgeRepository(engine),
        )
        assert reader.get_user_badges(user) == []


# ---------------------------------------------------------------------------
# Attendance
# ---------------------------------------------------------------------------
class TestAttendancePersistence:

    def test_recorded_attendance_survives_a_fresh_instance(
        self, engine: sqlalchemy.Engine
    ):
        attendee, event = uuid.uuid4(), uuid.uuid4()

        writer = attendance.AttendanceService(
            attendance.SQLAlchemyAttendanceRepository(engine)
        )
        writer.record_attendance(attendee, event)

        reader = attendance.AttendanceService(
            attendance.SQLAlchemyAttendanceRepository(engine)
        )
        assert reader.has_attended(attendee, event)
        assert event in [
            record.event_id for record in reader.get_attendances(attendee)
        ]

    def test_withdrawn_attendance_survives_a_fresh_instance(
        self, engine: sqlalchemy.Engine
    ):
        attendee, event = uuid.uuid4(), uuid.uuid4()

        writer = attendance.AttendanceService(
            attendance.SQLAlchemyAttendanceRepository(engine)
        )
        writer.record_attendance(attendee, event)
        writer.withdraw_attendance(attendee, event)

        reader = attendance.AttendanceService(
            attendance.SQLAlchemyAttendanceRepository(engine)
        )
        assert not reader.has_attended(attendee, event)
