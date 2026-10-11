# api-core/tests/test_seed.py
"""Tests for the Python-side seed loader (api-core/src/seed.py).

Verifies that ``load_seed`` correctly deserialises every array from
``data/seed.json`` into its corresponding in-memory repository: entity counts,
field mapping, type correctness for datetime and enum fields, and basic error
handling.

Run from the repo root: ``python -m pytest api-core/tests/``
"""

import datetime
import json
import os
import uuid

import pytest

import bootstrap
import seed as seed_module
from badges.predicates import IPredicate
from friends import FriendshipStatus

# ---------------------------------------------------------------------------
# Seed file path — absolute so tests work regardless of working directory
# ---------------------------------------------------------------------------

SEED_PATH = os.path.normpath(
    os.path.join(os.path.dirname(__file__), "../../data/seed.json")
)

# ---------------------------------------------------------------------------
# Entity UUIDs pinned to specific seed records (for spot-check assertions)
# ---------------------------------------------------------------------------

ALICE = uuid.UUID("20000000-0000-4000-a000-000000000001")
BEN   = uuid.UUID("20000000-0000-4000-a000-000000000002")
ELLA  = uuid.UUID("20000000-0000-4000-a000-000000000005")

FRESHERS_FAIR = uuid.UUID("40000000-0000-4000-a000-000000000001")

SOCIAL_BUTTERFLY_ID = uuid.UUID("60000000-0000-4000-a000-000000000001")
REGULAR_ID          = uuid.UUID("60000000-0000-4000-a000-000000000004")

# ---------------------------------------------------------------------------
# Expected counts — derived from seed.json so count tests survive seed changes
# ---------------------------------------------------------------------------

with open(SEED_PATH) as _f:
    _seed = json.load(_f)

_EXPECTED_ATTENDANCE  = len(_seed["attendance"])
_EXPECTED_BADGES      = len(_seed["badges"])
_EXPECTED_FRIENDSHIPS = len(_seed["friendships"])

# ---------------------------------------------------------------------------
# Fixture
# ---------------------------------------------------------------------------


@pytest.fixture(scope="module")
def seeded():
    """Services graph with seed data loaded once for all tests in this module."""
    services = bootstrap.bootstrap(register=False)
    seed_module.load_seed(services, seed_path=SEED_PATH)
    return services


# ---------------------------------------------------------------------------
# A. Entity counts
# ---------------------------------------------------------------------------


def test_loads_all_attendance_records(seeded):
    assert len(seeded.attendance_repo.find_all()) == _EXPECTED_ATTENDANCE


def test_loads_all_badges(seeded):
    assert len(seeded.badge_repo.find_all()) == _EXPECTED_BADGES


def test_loads_all_friendships(seeded):
    assert len(seeded.friendship_repo.find_all()) == _EXPECTED_FRIENDSHIPS


# ---------------------------------------------------------------------------
# B. Attendance field mapping
# ---------------------------------------------------------------------------


def test_attendance_alice_at_freshers_fair_fields(seeded):
    record = seeded.attendance_repo.find_by_id((ALICE, FRESHERS_FAIR))
    assert record is not None
    assert record.attendee_id == ALICE
    assert record.event_id == FRESHERS_FAIR
    assert record.recorded_at == datetime.datetime(
        2026, 9, 21, 10, 5, 0, tzinfo=datetime.timezone.utc
    )


def test_attendance_recorded_at_is_datetime(seeded):
    record = seeded.attendance_repo.find_by_id((ALICE, FRESHERS_FAIR))
    assert isinstance(record.recorded_at, datetime.datetime)


# ---------------------------------------------------------------------------
# C. Badge field mapping
# ---------------------------------------------------------------------------


def test_social_butterfly_badge_fields(seeded):
    badge = seeded.badge_repo.find_by_id(SOCIAL_BUTTERFLY_ID)
    assert badge is not None
    assert badge.name == "Social Butterfly"
    assert badge.description == "Make 3 friends on the platform."
    assert badge.repeatable is False


def test_regular_badge_is_repeatable(seeded):
    badge = seeded.badge_repo.find_by_id(REGULAR_ID)
    assert badge is not None
    assert badge.repeatable is True


def test_badge_award_condition_is_predicate(seeded):
    badge = seeded.badge_repo.find_by_id(SOCIAL_BUTTERFLY_ID)
    assert isinstance(badge.award_condition, IPredicate)


# ---------------------------------------------------------------------------
# D. Friendship field mapping
# ---------------------------------------------------------------------------


def test_alice_ben_friendship_fields(seeded):
    friendship = seeded.friendship_repo.find_by_id(frozenset({ALICE, BEN}))
    assert friendship is not None
    assert friendship.user_id == ALICE
    assert friendship.friend_id == BEN
    assert friendship.created_at == datetime.datetime(
        2026, 9, 21, 12, 30, 0, tzinfo=datetime.timezone.utc
    )
    assert friendship.friendship_status == FriendshipStatus.ACCEPTED


def test_friendship_created_at_is_datetime(seeded):
    friendship = seeded.friendship_repo.find_by_id(frozenset({ALICE, BEN}))
    assert isinstance(friendship.created_at, datetime.datetime)


def test_pending_friendship_has_correct_status(seeded):
    friendship = seeded.friendship_repo.find_by_id(frozenset({BEN, ELLA}))
    assert friendship is not None
    assert friendship.friendship_status == FriendshipStatus.PENDING


# ---------------------------------------------------------------------------
# E. Error handling
# ---------------------------------------------------------------------------


def test_missing_file_raises_file_not_found():
    services = bootstrap.bootstrap(register=False)
    with pytest.raises(FileNotFoundError):
        seed_module.load_seed(services, seed_path="/nonexistent/path/seed.json")


def test_invalid_json_raises(tmp_path):
    bad_file = tmp_path / "seed.json"
    bad_file.write_text("this is not json")
    services = bootstrap.bootstrap(register=False)
    with pytest.raises(Exception): # noqa: B017
        seed_module.load_seed(services, seed_path=str(bad_file))
